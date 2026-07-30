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
            LOG.debug(format("Decoding document using codec for %s'", morphiaCodec.getEntityModel().getType().getName()));
            MorphiaInstanceCreator instanceCreator = getInstanceCreator();
            T instance = (T) instanceCreator.getInstance();

            DecodeSession session = DecodeSession.current();
            Object prereadId = null;
            if (session != null) {
                prereadId = peekId(reader);
                if (prereadId != null) {
                    session.register(classModel.collectionName(), prereadId, instance);
                }
            }

            decodeProperties(reader, decoderContext, instanceCreator, classModel);

            if (session != null && prereadId == null) {
                PropertyModel idProp = classModel.getIdProperty();
                if (idProp != null) {
                    Object id = morphiaCodec.getDatastore().getMapper().getId(instance);
                    if (id != null) {
                        session.register(classModel.collectionName(), id, instance);
                    }
                }
            }

            return (T) instanceCreator.getInstance();
        } else {
            entity = getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.discriminatorKey(),
                    morphiaCodec.getRegistry(), morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
                    .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
        }

        return entity;
    }

    protected void decodeModel(BsonReader reader, DecoderContext decoderContext,
            MorphiaInstanceCreator instanceCreator, @Nullable PropertyModel model) {

        if (model != null) {
            final BsonReaderMark mark = reader.getMark();
            try {
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    Object value = decoderContext.decodeWithChildContext(model.getCodec(), reader);
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
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.discriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodeModel(reader, decoderContext, instanceCreator, classModel.getProperty(name));
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
            String idName = classModel.getIdProperty() != null
                    ? classModel.getIdProperty().getMappedName()
                    : "_id";
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
