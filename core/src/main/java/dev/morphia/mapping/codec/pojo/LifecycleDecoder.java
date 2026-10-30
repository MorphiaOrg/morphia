package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
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
    /**
     * creates the decoder
     *
     * @param codec the codec
     */
    // stands in for a discriminator that isn't stored as a string; compared by identity
    private static final String NOT_A_STRING = new String("not a string");

    public LifecycleDecoder(MorphiaCodec<T> codec) {
        super(codec);
    }

    /**
     * @param model  the entity's model
     * @param mapper the mapper
     * @return true if decoding the entity runs callbacks: the model or a global listener handles {@code @PreLoad} or
     *         {@code @PostLoad}
     */
    public static boolean isNeeded(EntityModel model, Mapper mapper) {
        return LifecycleEvents.handles(model, mapper, PreLoad.class, PostLoad.class);
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        CodecPlan plan = getMorphiaCodec().getPlan();
        Mapper mapper = getMorphiaCodec().getDatastore().getMapper();
        if (LifecycleEvents.needDocument(model, mapper, PreLoad.class, PostLoad.class)) {
            // a callback takes the document, whatever subtype it turns out to be
            return decodeDocument(reader, decoderContext);
        }
        if (model.useDiscriminator()) {
            String discriminator = peekDiscriminator(reader, model.discriminatorKey());
            if (discriminator == NOT_A_STRING) {
                // let the document path fail on it as it always has
                return decodeDocument(reader, decoderContext);
            }
            if (discriminator != null) {
                MorphiaCodec<?> codec = codecFor(discriminator);
                model = codec.getEntityModel();
                plan = codec.getPlan();
                if (model != getMorphiaCodec().getEntityModel()
                        && LifecycleEvents.needDocument(model, mapper, PreLoad.class, PostLoad.class)) {
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
            String discriminator = document.getString(model.discriminatorKey());
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
                : document != null ? document.get(idProperty.getMappedName()) : peekId(reader, model);

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
        if (!eager && id == null && session != null && idProperty != null) {
            // as EntityDecoder does when no id could be read ahead: cache the entity under its mapped id. one read
            // ahead was put in flight above and has been cached by complete()
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
     * @return the document's discriminator, null if it has none, or {@link #NOT_A_STRING} if it isn't a string. The
     *         reader is left where it was.
     */
    @Nullable
    private static String peekDiscriminator(BsonReader reader, String discriminatorKey) {
        BsonReaderMark mark = reader.getMark();
        try {
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (discriminatorKey.equals(reader.readName())) {
                    return reader.getCurrentBsonType() == BsonType.STRING ? reader.readString() : NOT_A_STRING;
                }
                reader.skipValue();
            }
            return null;
        } finally {
            mark.reset();
        }
    }
}
