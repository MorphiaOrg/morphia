package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
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
import org.bson.types.ObjectId;
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
    private static final DecoderContext DEFAULT_CONTEXT = DecoderContext.builder().build();

    private final MorphiaCodec<T> morphiaCodec;
    private final EntityModel classModel;
    // decodes a pre-read _id the way references read theirs, so the session keys match; resolved on first use.
    // volatile because decoders are shared across threads
    private volatile Codec<Object> idKeyCodec;

    protected EntityDecoder(MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
        classModel = morphiaCodec.getEntityModel();
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        if (decoderContext.hasCheckedDiscriminator() || !classModel.useDiscriminator()) {
            reader.readStartDocument();
            return decodeFields(reader, decoderContext, null);
        }
        return decodeWithDiscriminator(reader);
    }

    /**
     * Reads the discriminator in the same pass as the properties when the document starts the way Morphia writes it: the
     * {@code _id}, then the discriminator. Decoding then carries on from there, with this decoder or the subtype's. Any
     * other layout, and any subtype whose decoder does more than this one, goes back to the start and finds the codec
     * first.
     */
    @SuppressWarnings("unchecked")
    private T decodeWithDiscriminator(BsonReader reader) {
        BsonReaderMark start = reader.getMark();
        reader.readStartDocument();
        String discriminatorKey = classModel.discriminatorKey();
        Codec<? extends T> codec = null;
        Object id = null;
        BsonType idType = null;
        try {
            BsonType type;
            while ((type = reader.readBsonType()) != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (discriminatorKey.equals(name)) {
                    if (type == BsonType.STRING) {
                        codec = morphiaCodec.codecForDiscriminator(reader.readString());
                    }
                    break;
                }
                if (idType != null || !isIdName(name) || defaultIdClass(type) == null) {
                    break;
                }
                id = readSimpleId(reader, type);
                idType = type;
            }
        } catch (RuntimeException e) {
            // finding the codec the long way reports it
            codec = null;
        }
        EntityDecoder<? extends T> decoder = codec != null ? continuingDecoder(codec) : null;
        if (decoder != null && (idType == null || decoder.takesId(id, idType))) {
            Object readId = id;
            if (DecodeSession.current() != null) {
                return decoder.decodeFields(reader, CHECKED_DISCRIMINATOR, readId);
            }
            // as MorphiaCodec.decode would have for the codec
            return DecodeSession.forConfig(morphiaCodec.getDatastore().getMapper().getConfig())
                    .decoding(() -> decoder.decodeFields(reader, CHECKED_DISCRIMINATOR, readId));
        }
        start.reset();
        return getCodecFromDocument(reader, true, discriminatorKey, morphiaCodec).decode(reader, CHECKED_DISCRIMINATOR);
    }

    /**
     * @return the decoder that can pick up a document part-way through for the codec, or null if it decodes some other way
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private EntityDecoder<? extends T> continuingDecoder(Codec<? extends T> codec) {
        if (codec == morphiaCodec) {
            return this;
        }
        if (codec instanceof MorphiaCodec<?> other) {
            EntityDecoder<?> decoder = other.getDecoder();
            if (decoder.getClass() == EntityDecoder.class) {
                return (EntityDecoder<? extends T>) decoder;
            }
        }
        return null;
    }

    /**
     * @return true if an id read ahead as {@code readSimpleId} reads it is the value this model's id property decodes to
     */
    private boolean takesId(Object id, BsonType type) {
        PropertyModel idProperty = classModel.getIdProperty();
        return idProperty == null || idProperty.getType() == id.getClass() && morphiaCodec.getPlan().readsDirectly(idProperty, type);
    }

    /**
     * Decodes the rest of a document whose start has been read, registering the entity with the session as soon as its id
     * is known: before any later property, and so any reference, is decoded.
     *
     * @param id the {@code _id}, if it was read before the discriminator; it isn't in the rest of the document
     */
    @SuppressWarnings("unchecked")
    private T decodeFields(BsonReader reader, DecoderContext decoderContext, @Nullable Object id) {
        LOG.debug("Decoding document using codec for {}", classModel.getType().getName());
        MorphiaInstanceCreator instanceCreator = getInstanceCreator();
        CodecPlan plan = morphiaCodec.getPlan();
        DecodeSession session = DecodeSession.current();
        PropertyModel idProperty = classModel.getIdProperty();
        boolean useDiscriminator = classModel.useDiscriminator();
        String discriminatorKey = classModel.discriminatorKey();

        // Publishing the instance before its properties are decoded is what lets a reference cycle terminate, but it is
        // only possible when the creator can hand out an instance without the decoded values. Creators that feed values to
        // a constructor are registered after decoding instead: they still deduplicate, but a cycle through them cannot be
        // broken, so they are marked as under construction and a reference back to one fails fast instead of recursing.
        boolean tracking = session != null && idProperty != null;
        boolean registered = false;
        boolean eager = false;
        Object key = null;
        boolean decoded = false;
        try {
            if (id != null && idProperty != null) {
                if (tracking) {
                    key = id;
                    eager = register(session, instanceCreator, key);
                    registered = true;
                }
                instanceCreator.set(id, idProperty);
            }
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (useDiscriminator && discriminatorKey.equals(name)) {
                    reader.skipValue();
                    continue;
                }
                PropertyModel model = classModel.getProperty(name);
                if (tracking && !registered) {
                    registered = true;
                    if (model == idProperty) {
                        key = decodeId(reader, decoderContext, instanceCreator, plan, idProperty);
                        eager = register(session, instanceCreator, key);
                        continue;
                    }
                    // a property comes before the id, so read the id ahead before decoding anything that might refer back
                    key = scanForId(reader);
                    eager = register(session, instanceCreator, key);
                }
                decodeModel(reader, decoderContext, instanceCreator, plan, model);
            }
            reader.readEndDocument();
            decoded = true;
        } finally {
            if (key != null) {
                if (decoded) {
                    session.complete(classModel.collectionName(), key);
                } else {
                    session.discard(classModel.collectionName(), key);
                }
            }
        }

        T instance = (T) instanceCreator.getInstance();
        if (tracking && !eager) {
            // prefer the id as stored, which is what references look entities up by; the mapped value can differ for
            // converted id types
            Object stored = key != null ? key : morphiaCodec.getDatastore().getMapper().getId(instance);
            if (stored != null) {
                session.register(classModel.collectionName(), stored, instance);
            }
        }
        return instance;
    }

    /**
     * @return true if the instance was published before its properties are decoded
     */
    private boolean register(DecodeSession session, MorphiaInstanceCreator instanceCreator, @Nullable Object key) {
        if (key == null) {
            return false;
        }
        if (instanceCreator.isEagerInstanceSafe()) {
            session.registerInFlight(classModel.collectionName(), key, instanceCreator.getInstance());
            return true;
        }
        session.registerConstructing(classModel.collectionName(), key);
        return false;
    }

    /**
     * Decodes the id property, whose name has just been read.
     *
     * @return the id as stored, decoded the way references read theirs so the session keys match, or null if it can't be
     *         read
     */
    @Nullable
    private Object decodeId(BsonReader reader, DecoderContext decoderContext, MorphiaInstanceCreator instanceCreator,
            CodecPlan plan, PropertyModel idProperty) {
        BsonType type = reader.getCurrentBsonType();
        if (idProperty.getType() == defaultIdClass(type) && plan.readsDirectly(idProperty, type)) {
            // the property's codec reads the value just as references do, so one read serves as both
            Object id = decoderContext.decodeWithChildContext(plan.codec(idProperty), reader);
            instanceCreator.set(id, idProperty);
            return id;
        }
        BsonReaderMark mark = reader.getMark();
        Object key;
        try {
            key = idKeyCodec().decode(reader, DEFAULT_CONTEXT);
        } catch (Exception e) {
            LOG.debug("Could not read _id for DecodeSession on {}; cycle detection may not apply",
                    classModel.getType().getSimpleName(), e);
            key = null;
        }
        mark.reset();
        decodeModel(reader, decoderContext, instanceCreator, plan, idProperty);
        return key;
    }

    /**
     * Finds the id further on in the document, leaving the reader at the value of the property whose name was just read.
     *
     * @return the id, decoded the way references read theirs, or null if the document has none or it can't be read
     */
    @Nullable
    private Object scanForId(BsonReader reader) {
        BsonReaderMark mark = reader.getMark();
        try {
            reader.skipValue();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (isIdName(reader.readName())) {
                    return idKeyCodec().decode(reader, DEFAULT_CONTEXT);
                }
                reader.skipValue();
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

    private boolean isIdName(String name) {
        PropertyModel idProperty = classModel.getIdProperty();
        return "_id".equals(name) || idProperty != null && name.equals(idProperty.getMappedName());
    }

    /**
     * @return the class an id stored as this type is read as, by references and by a property of that class alike, or
     *         null for the types that aren't read ahead
     */
    @Nullable
    private static Class<?> defaultIdClass(BsonType type) {
        return switch (type) {
            case OBJECT_ID -> ObjectId.class;
            case STRING -> String.class;
            case INT32 -> Integer.class;
            case INT64 -> Long.class;
            default -> null;
        };
    }

    private static Object readSimpleId(BsonReader reader, BsonType type) {
        return switch (type) {
            case OBJECT_ID -> reader.readObjectId();
            case STRING -> reader.readString();
            case INT32 -> reader.readInt32();
            case INT64 -> reader.readInt64();
            default -> throw new IllegalArgumentException(type.toString());
        };
    }

    private Codec<Object> idKeyCodec() {
        Codec<Object> codec = idKeyCodec;
        if (codec == null) {
            codec = morphiaCodec.getRegistry().get(Object.class);
            idKeyCodec = codec;
        }
        return codec;
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

        if (model == null) {
            reader.skipValue();
            return;
        }
        BsonType type = reader.getCurrentBsonType();
        if (type == BsonType.NULL) {
            reader.readNull();
            return;
        }
        if (plan.readsDirectly(model, type)) {
            // the codec can't fail part-way on this type, so there's nothing to rewind and no mark to take
            instanceCreator.set(decoderContext.decodeWithChildContext(plan.codec(model), reader), model);
            return;
        }
        final BsonReaderMark mark = reader.getMark();
        try {
            Object value = decoderContext.decodeWithChildContext(plan.codec(model), reader);
            instanceCreator.set(value, model);
        } catch (BsonInvalidOperationException e) {
            mark.reset();
            final Object value = morphiaCodec.getRegistry().get(Object.class).decode(reader, decoderContext);
            // not every creator converts (the refresh codec's sets values directly), and a constructor parameter can
            // need the property's type as a step on the way. ConstructorCreator and setValue skip the second
            // conversion once the value fits.
            instanceCreator.set(morphiaCodec.getConversions().convert(value, model.getType()), model);
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
                reader.skipValue();
            } else {
                decodeModel(reader, decoderContext, instanceCreator, plan, classModel.getProperty(name));
            }
        }
        reader.readEndDocument();
    }

    protected Codec<T> getCodecFromDocument(BsonReader reader, boolean useDiscriminator, String discriminatorKey,
            Codec<T> defaultCodec) {
        Codec<T> codec = null;
        if (useDiscriminator) {
            BsonReaderMark mark = reader.getMark();
            try {
                reader.readStartDocument();
                while (codec == null && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    if (discriminatorKey.equals(reader.readName())) {
                        codec = (Codec<T>) morphiaCodec.codecForDiscriminator(reader.readString());
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

    /**
     * Reads a document's {@code _id} ahead of decoding it, leaving the reader where it was.
     *
     * @param reader   the reader, positioned at the document
     * @param model    the model the document is decoded as, which must have an id property
     * @param entities whether to read an id that decodes as an entity: one stored with a discriminator, which is how the
     *                 id reader tells. Reading one runs that entity's load callbacks
     * @return the id, decoded the way references read theirs, or null if the document has none, it can't be read, or
     *         it's an entity and {@code entities} is false
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    protected Object peekId(BsonReader reader, EntityModel model, boolean entities) {
        BsonReaderMark mark = reader.getMark();
        try {
            reader.readStartDocument();
            String idName = model.getIdProperty().getMappedName();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if ("_id".equals(name) || name.equals(idName)) {
                    if (!entities && reader.getCurrentBsonType() == BsonType.DOCUMENT && decodesAsEntity(reader)) {
                        return null;
                    }
                    return idKeyCodec().decode(reader, DEFAULT_CONTEXT);
                } else {
                    reader.skipValue();
                }
            }
            return null;
        } catch (Exception e) {
            LOG.debug("Could not pre-read _id for DecodeSession on {}; cycle detection may not apply",
                    model.getType().getSimpleName(), e);
            return null;
        } finally {
            mark.reset();
        }
    }

    /**
     * @param reader the reader, positioned at an id stored as a document, which it's left at
     * @return true if the id decodes as an entity: like {@code ObjectCodec}, which reads ids ahead, it's stored with the
     *         discriminator key. One without it, such as an entity mapped with {@code useDiscriminator = false}, is read as
     *         a plain {@code Document}
     */
    private boolean decodesAsEntity(BsonReader reader) {
        String discriminatorKey = morphiaCodec.getDatastore().getMapper().getConfig().discriminatorKey();
        BsonReaderMark mark = reader.getMark();
        try {
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (discriminatorKey.equals(reader.readName())) {
                    return true;
                }
                reader.skipValue();
            }
            return false;
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
