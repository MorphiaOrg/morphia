package dev.morphia.mapping.codec.pojo;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.writer.DocumentWriter;

import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;

/**
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public class LifecycleEncoder<T> extends EntityEncoder<T> {
    /**
     * Creates a new encoder
     *
     * @param morphiaCodec the codec
     */
    public LifecycleEncoder(MorphiaCodec<T> morphiaCodec) {
        super(morphiaCodec);
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        MorphiaDatastore datastore = getMorphiaCodec().getDatastore();

        Document document = new Document();
        model.callLifecycleMethods(PrePersist.class, value, document, datastore);

        final DocumentWriter documentWriter = new DocumentWriter(datastore.getMapper().getConfig(), document);
        super.encode(documentWriter, value, encoderContext);
        document = documentWriter.getDocument();
        model.callLifecycleMethods(PostPersist.class, value, document, datastore);

        getMorphiaCodec().getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

}
