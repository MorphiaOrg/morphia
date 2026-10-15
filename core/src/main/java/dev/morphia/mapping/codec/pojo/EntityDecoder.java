package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DiscriminatorLookup;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;

import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.codecs.Codec;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * @param <T> the entity type
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class EntityDecoder<T> implements Decoder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityDecoder.class);
    private static final DecoderContext CHECKED_DISCRIMINATOR = DecoderContext.builder().checkedDiscriminator(true).build();

    private final MorphiaCodec<T> morphiaCodec;
    private final EntityModel classModel;

    protected EntityDecoder(MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
        classModel = morphiaCodec.getEntityModel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        T entity;
        if (decoderContext.hasCheckedDiscriminator()) {
            LOG.debug("Decoding document using codec for {}", morphiaCodec.getEntityModel().getType().getName());
            MorphiaInstanceCreator instanceCreator = getInstanceCreator();
            DecodeSession session = DecodeSession.current();

            // Publishing the instance before its properties are decoded is what lets a reference cycle
            // terminate, but it is only possible when the creator can hand out an instance without the
            // decoded values. Creators that feed values to a constructor are registered after decoding
            // instead: they still deduplicate, but a cycle through them cannot be broken, so they are marked as
            // under construction and a reference back to one fails fast instead of recursing.
            Object prereadId = session != null && classModel.getIdProperty() != null ? peekId(reader) : null;
            boolean eager = prereadId != null && instanceCreator.isEagerInstanceSafe();
            if (eager) {
                session.registerInFlight(classModel.collectionName(), prereadId, instanceCreator.getInstance());
            } else if (prereadId != null) {
                session.registerConstructing(classModel.collectionName(), prereadId);
            }

            boolean decoded = false;
            try {
                decodeProperties(reader, decoderContext, instanceCreator, morphiaCodec.getPlan());
                decoded = true;
            } finally {
                if (prereadId != null) {
                    if (decoded) {
                        session.complete(classModel.collectionName(), prereadId);
                    } else {
                        session.discard(classModel.collectionName(), prereadId);
                    }
                }
            }

            T instance = (T) instanceCreator.getInstance();

            if (session != null && !eager && classModel.getIdProperty() != null) {
                // prefer the id as stored, which is what references look entities up by; the mapped value can differ
                // for converted id types
                Object id = prereadId != null ? prereadId : morphiaCodec.getDatastore().getMapper().getId(instance);
                if (id != null) {
                    session.register(classModel.collectionName(), id, instance);
                }
            }

            return instance;
        } else {
            entity = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.discriminatorKey(),
                    morphiaCodec.getRegistry(), morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
                    .decode(reader, CHECKED_DISCRIMINATOR);
        }

        return entity;
    }

    protected void decodeModel(BsonReader reader, DecoderContext decoderContext,
            MorphiaInstanceCreator instanceCreator, @Nullable PropertyModel model) {
        decodeModel(reader, decoderContext, instanceCreator, morphiaCodec.getPlan(), model);
    }

    /**
     * Decodes a property with the codec the given plan holds for it.
     *
     * @param reader          the reader
     * @param decoderContext  the context
     * @param instanceCreator the creator to give the value to
     * @param plan            the plan of the codec for the model the property belongs to
     * @param model           the property, or null to skip the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void decodeModel(BsonReader reader, DecoderContext decoderContext,
            MorphiaInstanceCreator instanceCreator, CodecPlan plan, @Nullable PropertyModel model) {

        if (model != null) {
            final BsonReaderMark mark = reader.getMark();
            try {
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    Object value = decoderContext.decodeWithChildContext(plan.codec(model), reader);
                    instanceCreator.set(value, model);
                }
            } catch (BsonInvalidOperationException e) {
                mark.reset();
                final Object value = morphiaCodec.getRegistry().get(Object.class).decode(reader, decoderContext);
                instanceCreator.set(morphiaCodec.getConversions().convert(value, model.getTypeData().getType()), model);
            }
        } else {
            reader.skipValue();
        }
    }

    protected void decodeProperties(BsonReader reader, DecoderContext decoderContext,
            MorphiaInstanceCreator instanceCreator, EntityModel classModel) {
        CodecPlan plan = morphiaCodec.getPlan();
        if (plan.model() != classModel) {
            plan = ((MorphiaCodec<?>) morphiaCodec.getRegistry().get(classModel.getType())).getPlan();
        }
        decodeProperties(reader, decoderContext, instanceCreator, plan);
    }

    /**
     * Decodes a document's properties with the codecs a plan holds.
     *
     * @param reader          the reader
     * @param decoderContext  the context
     * @param instanceCreator the creator to give the values to
     * @param plan            the plan of the codec for the model being decoded
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void decodeProperties(BsonReader reader, DecoderContext decoderContext,
            MorphiaInstanceCreator instanceCreator, CodecPlan plan) {
        EntityModel classModel = plan.model();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.discriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodeModel(reader, decoderContext, instanceCreator, plan, classModel.getProperty(name));
            }
        }
        reader.readEndDocument();
    }

    protected Codec<T> getCodecFromDocument(BsonReader reader, boolean useDiscriminator, String discriminatorKey,
            CodecRegistry registry, DiscriminatorLookup discriminatorLookup,
            Codec<T> defaultCodec) {
        Codec<T> codec = null;
        if (useDiscriminator) {
            BsonReaderMark mark = reader.getMark();
            try {
                reader.readStartDocument();
                while (codec == null && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    if (discriminatorKey.equals(reader.readName())) {
                        codec = (Codec<T>) registry.get(discriminatorLookup.lookup(reader.readString()));
                    } else {
                        reader.skipValue();
                    }
                }
            } catch (Exception e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding errored with: %s",
                        morphiaCodec.getEntityModel().getName(), e.getMessage()), e);
            } finally {
                mark.reset();
            }
        }
        return codec != null ? codec : defaultCodec;
    }

    @Nullable
    private Object peekId(BsonReader reader) {
        BsonReaderMark mark = reader.getMark();
        try {
            reader.readStartDocument();
            String idName = classModel.getIdProperty().getMappedName();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if ("_id".equals(name) || name.equals(idName)) {
                    return morphiaCodec.getRegistry()
                            .get(Object.class)
                            .decode(reader, DecoderContext.builder().build());
                } else {
                    reader.skipValue();
                }
            }
            return null;
        } catch (Exception e) {
            LOG.debug("Could not pre-read _id for DecodeSession on {}; cycle detection may not apply",
                    classModel.getType().getSimpleName(), e);
            return null;
        } finally {
            mark.reset();
        }
    }

    protected MorphiaInstanceCreator getInstanceCreator() {
        return classModel.getInstanceCreator(morphiaCodec.getConversions());
    }

    protected MorphiaCodec<T> getMorphiaCodec() {
        return morphiaCodec;
    }
}
