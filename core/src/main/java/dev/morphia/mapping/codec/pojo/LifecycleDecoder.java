package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import static java.lang.String.format;

/**
 * @param <T> the type
 * @hidden
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public class LifecycleDecoder<T> extends EntityDecoder<T> {
    // whether a listener of the entity, or a configured global listener, takes the Document; fixed once the mapper exists
    private final boolean needsDocument;

    /**
     * creates the decoder
     *
     * @param codec the codec
     */
    public LifecycleDecoder(MorphiaCodec<T> codec) {
        super(codec);
        needsDocument = LifecycleEvents.needDocument(codec.getEntityModel(), codec.getDatastore().getMapper(), PreLoad.class,
                PostLoad.class);
    }

    /**
     * @return true if a listener of the entity or a configured global listener takes the {@code Document}, so this builds
     *         one
     */
    boolean needsDocument() {
        return needsDocument;
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        CodecPlan plan = getMorphiaCodec().getPlan();
        if (needsDocument) {
            // a callback takes the document, whatever subtype it turns out to be
            return decodeDocument(reader, decoderContext);
        }
        if (!model.useDiscriminator() && !needsId(model)) {
            // nothing to know before the properties
            return decode(reader, decoderContext, model, plan, null, null, false);
        }
        if (getClass() != LifecycleDecoder.class) {
            // a subclass may customize decodeProperties or decodeModel, which reading the leading fields would bypass
            return decodeReadingAhead(reader, decoderContext);
        }
        BsonReaderMark start = reader.getMark();
        reader.readStartDocument();
        T entity = decodeLeadingFields(reader, decoderContext);
        if (entity != null) {
            return entity;
        }
        start.reset();
        return decodeReadingAhead(reader, decoderContext);
    }

    /**
     * Reads the {@code _id} and the discriminator in the same pass as the properties when the document starts with them,
     * as Morphia writes it, and decodes the rest of it.
     *
     * @return the entity, or null if the document starts some other way, which leaves the reader part-way through it
     */
    @Nullable
    private T decodeLeadingFields(BsonReader reader, DecoderContext decoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        String discriminatorKey = model.discriminatorKey();
        boolean discriminated = !model.useDiscriminator();
        MorphiaCodec<?> codec = getMorphiaCodec();
        Object id = null;
        BsonType idType = null;
        BsonType type;
        while ((type = reader.readBsonType()) != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (!discriminated && discriminatorKey.equals(name)) {
                if (type == BsonType.STRING) {
                    codec = codecFor(reader.readString());
                } else if (type == BsonType.NULL) {
                    // as if it were absent, as the Document path reads it
                    reader.readNull();
                } else {
                    throw notAString(discriminatorKey, type);
                }
                discriminated = true;
            } else if (idType == null && isIdName(name) && defaultIdClass(type) != null) {
                id = readSimpleId(reader, type);
                idType = type;
            } else {
                // a property: what's still unknown may come later
                return null;
            }
            if (discriminated && (idType != null || !needsId(codec.getEntityModel()))) {
                EntityModel subtype = codec.getEntityModel();
                if (subtype != model) {
                    // the discriminator has been read, so the subtype must skip it too rather than map it as a property
                    if (!subtype.useDiscriminator() || !subtype.discriminatorKey().equals(discriminatorKey)) {
                        return null;
                    }
                    if (LifecycleEvents.needDocument(subtype, codec.getDatastore().getMapper(), PreLoad.class, PostLoad.class)) {
                        return null;
                    }
                }
                if (idType != null && !takesId(subtype, codec.getPlan(), idType)) {
                    return null;
                }
                return decode(reader, decoderContext, subtype, codec.getPlan(), null, id, true);
            }
        }
        // a document of nothing but leading fields; reading it again is simplest
        return null;
    }

    /**
     * Finds the subtype, and the id, by reading ahead, for documents that don't start as Morphia writes them.
     */
    private T decodeReadingAhead(BsonReader reader, DecoderContext decoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        CodecPlan plan = getMorphiaCodec().getPlan();
        if (model.useDiscriminator()) {
            String discriminator = peekDiscriminator(reader, model.discriminatorKey());
            if (discriminator != null) {
                MorphiaCodec<?> codec = codecFor(discriminator);
                model = codec.getEntityModel();
                plan = codec.getPlan();
                if (model != getMorphiaCodec().getEntityModel()
                        && LifecycleEvents.needDocument(model, getMorphiaCodec().getDatastore().getMapper(), PreLoad.class,
                                PostLoad.class)) {
                    // the subtype has a callback that takes the document
                    return decodeDocument(reader, decoderContext);
                }
            }
        }
        // no callback looks at the document, so read the entity straight from the reader rather than building one
        return decode(reader, decoderContext, model, plan, null, null, false);
    }

    /**
     * Carries on with a document whose start, {@code _id} and discriminator another decoder has read.
     */
    @Override
    T decodeFields(BsonReader reader, DecoderContext decoderContext, @Nullable Object id) {
        return decode(reader, decoderContext, getMorphiaCodec().getEntityModel(), getMorphiaCodec().getPlan(), null, id, true);
    }

    /**
     * Unlike {@link EntityDecoder}, this one doesn't look for the id among the rest of the properties, so it carries on
     * only once it has it, if the entity has one.
     */
    @Override
    boolean continuesAfter(@Nullable BsonType idType) {
        return idType == null ? getMorphiaCodec().getEntityModel().getIdProperty() == null : takesId(idType);
    }

    private static boolean needsId(EntityModel model) {
        return DecodeSession.current() != null && model.getIdProperty() != null;
    }

    private static boolean takesId(EntityModel model, CodecPlan plan, BsonType idType) {
        PropertyModel idProperty = model.getIdProperty();
        return idProperty != null && readsAsKey(plan, idProperty, idType);
    }

    /**
     * Decodes through a {@code Document}, for callbacks that take one.
     */
    private T decodeDocument(BsonReader reader, DecoderContext decoderContext) {
        Document document = getMorphiaCodec().getRegistry().get(Document.class).decode(reader, decoderContext);
        EntityModel model = getMorphiaCodec().getEntityModel();
        CodecPlan plan = getMorphiaCodec().getPlan();
        if (model.useDiscriminator()) {
            Object stored = document.get(model.discriminatorKey());
            if (stored != null && !(stored instanceof String)) {
                throw notAString(model.discriminatorKey(), stored.getClass().getSimpleName());
            }
            String discriminator = (String) stored;
            if (discriminator != null) {
                MorphiaCodec<?> codec = codecFor(discriminator);
                model = codec.getEntityModel();
                plan = codec.getPlan();
            }
        }
        return decode(new DocumentReader(document, getMorphiaCodec().getConversions()), decoderContext, model, plan, document,
                null, false);
    }

    /**
     * Decodes an entity and runs its load callbacks.
     *
     * @param reader   the reader for the entity's properties: the original one, or one over {@code document}
     * @param document the document the callbacks get, or null if none of them takes one
     * @param readId   the {@code _id}, if it has been read already; it isn't in the rest of the document
     * @param started  true if the document's start, and any fields before the properties, have been read
     */
    @SuppressWarnings("unchecked")
    private T decode(BsonReader reader, DecoderContext decoderContext, EntityModel model, CodecPlan plan,
            @Nullable Document document, @Nullable Object readId, boolean started) {
        MorphiaDatastore datastore = getMorphiaCodec().getDatastore();
        MorphiaInstanceCreator instanceCreator = model.getInstanceCreator(getMorphiaCodec().getConversions());
        DecodeSession session = DecodeSession.current();
        PropertyModel idProperty = model.getIdProperty();
        Object id = session == null || idProperty == null
                ? null
                : document != null
                        ? document.get(idProperty.getMappedName())
                        : started
                                ? readId
                                // an id that decodes as an entity isn't read ahead: decoding it would run its own load
                                // callbacks an extra time, before this entity's. the document path never keyed one the way
                                // references do either. any other id, a Document or a Map included, is
                                : peekId(reader, model, false);

        // mirrors EntityDecoder: publish the instance before its properties are decoded so that reference cycles
        // through a lifecycle-aware entity terminate the same way they do for any other entity. a creator that needs
        // the decoded values for its constructor can't hand out an instance until they're all read, so its entity
        // gets both of its load callbacks once it's built
        boolean eager = instanceCreator.isEagerInstanceSafe();
        T entity = null;
        if (eager) {
            entity = (T) instanceCreator.getInstance();
            if (id != null) {
                session.registerInFlight(model.collectionName(), id, entity);
            }
        } else if (id != null) {
            session.registerConstructing(model.collectionName(), id);
        }

        // the entity stays in flight until its callbacks have run too: a failure anywhere discards it, and nothing
        // sees it as complete before @PostLoad has finished with it
        boolean decoded = false;
        try {
            if (eager) {
                model.callLifecycleMethods(PreLoad.class, entity, document, datastore);
            }
            if (readId != null) {
                // after @PreLoad, which sees the entity before any of its stored values, as it does when the id is decoded
                // with the rest
                instanceCreator.set(readId, idProperty);
            }
            if (started) {
                decodeRemainingProperties(reader, decoderContext, instanceCreator, plan);
            } else {
                decodeProperties(reader, decoderContext, instanceCreator, plan);
            }
            if (!eager) {
                entity = (T) instanceCreator.getInstance();
                if (id != null) {
                    // the instance exists now, so a callback that reaches it through a reference gets it rather than
                    // an unresolvable cycle
                    session.registerInFlight(model.collectionName(), id, entity);
                }
                model.callLifecycleMethods(PreLoad.class, entity, document, datastore);
            }
            model.callLifecycleMethods(PostLoad.class, entity, document, datastore);
            decoded = true;
        } finally {
            if (id != null) {
                if (decoded) {
                    session.complete(model.collectionName(), id);
                } else {
                    session.discard(model.collectionName(), id);
                }
            }
        }
        if (id == null && session != null && idProperty != null) {
            // as EntityDecoder does when no id could be read ahead: cache the entity under its mapped id, so later
            // references to it reuse it. one read ahead was put in flight above and has been cached by complete()
            Object key = datastore.getMapper().getId(entity);
            if (key != null) {
                session.register(model.collectionName(), key, entity);
            }
        }

        return entity;
    }

    private MorphiaCodec<?> codecFor(String discriminator) {
        Codec<?> codec = getMorphiaCodec().codecForDiscriminator(discriminator);
        if (codec instanceof MorphiaCodec<?> morphiaCodec) {
            return morphiaCodec;
        }
        throw new CodecConfigurationException(format("Non-entity class used as discriminator: '%s'.", discriminator));
    }

    /**
     * @return the document's discriminator, or null if it has none or it's stored as null. The reader is left where it was.
     * @throws CodecConfigurationException if the discriminator isn't stored as a string
     */
    private static CodecConfigurationException notAString(String discriminatorKey, Object storedAs) {
        return new CodecConfigurationException(format("The discriminator '%s' must be stored as a string, not as %s.",
                discriminatorKey, storedAs));
    }

    @Nullable
    private static String peekDiscriminator(BsonReader reader, String discriminatorKey) {
        BsonReaderMark mark = reader.getMark();
        try {
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (discriminatorKey.equals(reader.readName())) {
                    if (reader.getCurrentBsonType() == BsonType.NULL) {
                        // as if it were absent, as the Document path reads it
                        return null;
                    }
                    if (reader.getCurrentBsonType() != BsonType.STRING) {
                        throw notAString(discriminatorKey, reader.getCurrentBsonType());
                    }
                    return reader.readString();
                }
                reader.skipValue();
            }
            return null;
        } finally {
            mark.reset();
        }
    }
}
