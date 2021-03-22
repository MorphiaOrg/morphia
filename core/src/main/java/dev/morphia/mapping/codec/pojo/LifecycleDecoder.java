package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.bson.BsonReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

/**
 * @morphia.internal
 * @since 2.2
 */
public class LifecycleDecoder extends EntityDecoder {
    /**
     * creates the decoder
     *
     * @param codec the codec
     * @param <T>   the type
     */
    public <T> LifecycleDecoder(MorphiaCodec<T> codec) {
        super(codec);
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = getMorphiaCodec().getRegistry().get(Document.class).decode(reader, decoderContext);
        EntityModel model = getMorphiaCodec().getEntityModel();
        if (model.useDiscriminator()) {
            String discriminator = document.getString(model.getDiscriminatorKey());
            if (discriminator != null) {
                try {
                    Class<?> discriminatorClass = getMorphiaCodec().getDiscriminatorLookup().lookup(discriminator);
                    model = getMorphiaCodec().getMapper().getEntityModel(discriminatorClass);
                    // create morphia codec that corresponds to entity model
                    getMorphiaCodec().getRegistry().get(discriminatorClass);
                } catch (CodecConfigurationException e) {
                    // Ignore missing discriminator class, use default entity model
                }
            }
        }
        final MorphiaInstanceCreator instanceCreator = model.getInstanceCreator();
        Object entity = instanceCreator.getInstance();
        model.callLifecycleMethods(PreLoad.class, entity, document, getMorphiaCodec().getMapper());
        decodeProperties(new DocumentReader(document), decoderContext, instanceCreator, model);
        model.callLifecycleMethods(PostLoad.class, entity, document, getMorphiaCodec().getMapper());

        return entity;
    }

}
