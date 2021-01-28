package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.2
 */
public class LifecycleEncoder extends EntityEncoder {
    /**
     * Creates a new encoder
     *
     * @param morphiaCodec the codec
     * @param <T>          the type
     */
    public <T> LifecycleEncoder(MorphiaCodec<T> morphiaCodec) {
        super(morphiaCodec);
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        Mapper mapper = getMorphiaCodec().getMapper();

        Document document = new Document();
        model.callLifecycleMethods(PrePersist.class, value, document, mapper);

        final DocumentWriter documentWriter = new DocumentWriter(document);
        super.encode(documentWriter, value, encoderContext);
        document = documentWriter.getDocument();
        model.callLifecycleMethods(PostPersist.class, value, document, mapper);

        getMorphiaCodec().getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

}
