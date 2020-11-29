package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.bson.BsonReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;

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
        final MorphiaInstanceCreator instanceCreator = getInstanceCreator();
        Object entity = instanceCreator.getInstance();
        EntityModel model = getMorphiaCodec().getEntityModel();
        Document document = getMorphiaCodec().getRegistry().get(Document.class).decode(reader, decoderContext);

        model.callLifecycleMethods(PreLoad.class, entity, document, getMorphiaCodec().getMapper());
        decodeProperties(new DocumentReader(document), decoderContext, instanceCreator);
        model.callLifecycleMethods(PostLoad.class, entity, document, getMorphiaCodec().getMapper());

        return entity;
    }

}
