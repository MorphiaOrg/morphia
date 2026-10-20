package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;

import org.bson.BsonReader;
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
    public LifecycleDecoder(MorphiaCodec<T> codec) {
        super(codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = getMorphiaCodec().getRegistry().get(Document.class).decode(reader, decoderContext);
        EntityModel model = getMorphiaCodec().getEntityModel();
        CodecPlan plan = getMorphiaCodec().getPlan();
        if (model.useDiscriminator()) {
            String discriminator = document.getString(model.discriminatorKey());
            if (discriminator != null) {
                Class<?> discriminatorClass = getMorphiaCodec().getDiscriminatorLookup().lookup(discriminator);
                // need to load the codec to initialize cachedCodecs in field models
                Codec<?> codec = getMorphiaCodec().getRegistry().get(discriminatorClass);
                if (codec instanceof MorphiaCodec) {
                    model = ((MorphiaCodec<?>) codec).getEntityModel();
                    plan = ((MorphiaCodec<?>) codec).getPlan();
                } else {
                    throw new CodecConfigurationException(format("Non-entity class used as discriminator: '%s'.", discriminator));
                }
            }
        }
        final MorphiaInstanceCreator instanceCreator = model.getInstanceCreator(getMorphiaCodec().getConversions());
        T entity = (T) instanceCreator.getInstance();

        // mirrors EntityDecoder: publish the instance before its properties are decoded so that reference
        // cycles through a lifecycle-aware entity terminate the same way they do for any other entity
        DecodeSession session = DecodeSession.current();
        PropertyModel idProperty = model.getIdProperty();
        Object id = session != null && idProperty != null && instanceCreator.isEagerInstanceSafe()
                ? document.get(idProperty.getMappedName())
                : null;
        if (id != null) {
            session.registerInFlight(model.collectionName(), id, entity);
        }

        // the entity stays in flight until its callbacks have run too: a failure anywhere discards it, and nothing
        // sees it as complete before @PostLoad has finished with it
        boolean decoded = false;
        try {
            model.callLifecycleMethods(PreLoad.class, entity, document, getMorphiaCodec().getDatastore());
            decodeProperties(new DocumentReader(document, getMorphiaCodec().getConversions()), decoderContext, instanceCreator,
                    plan);
            model.callLifecycleMethods(PostLoad.class, entity, document, getMorphiaCodec().getDatastore());
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

        return entity;
    }

}
