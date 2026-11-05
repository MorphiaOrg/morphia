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
        return decode(reader, decoderContext, model, plan, null);
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
        return decode(new DocumentReader(document, getMorphiaCodec().getConversions()), decoderContext, model, plan, document);
    }

    /**
     * Decodes an entity and runs its load callbacks.
     *
     * @param reader   the reader for the entity's properties: the original one, or one over {@code document}
     * @param document the document the callbacks get, or null if none of them takes one
     */
    @SuppressWarnings("unchecked")
    private T decode(BsonReader reader, DecoderContext decoderContext, EntityModel model, CodecPlan plan,
            @Nullable Document document) {
        MorphiaDatastore datastore = getMorphiaCodec().getDatastore();
        MorphiaInstanceCreator instanceCreator = model.getInstanceCreator(getMorphiaCodec().getConversions());
        DecodeSession session = DecodeSession.current();
        PropertyModel idProperty = model.getIdProperty();
        Object id = session == null || idProperty == null
                ? null
                : document != null
                        ? document.get(idProperty.getMappedName())
                        // an id mapped as an entity isn't read ahead: decoding it would run its own load callbacks an
                        // extra time, before this entity's. the document path never keyed one the way references do
                        // either. any other id, a Document or a Map included, is
                        : peekId(reader, model, !datastore.getMapper().isMappable(idProperty.getType()));

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
            decodeProperties(reader, decoderContext, instanceCreator, plan);
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
     * @return the document's discriminator, or null if it has none. The reader is left where it was.
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
