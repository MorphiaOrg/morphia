package dev.morphia.mapping.codec.pojo;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.writer.DocumentWriter;

import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;

/**
 * @param <T> the entity type
 * @hidden
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public class LifecycleEncoder<T> extends EntityEncoder<T> {
    // whether a callback of the entity, or a configured global listener, takes the Document; fixed once the mapper exists
    private final boolean needsDocument;

    /**
     * Creates a new encoder
     *
     * @param morphiaCodec the codec
     */
    public LifecycleEncoder(MorphiaCodec<T> morphiaCodec) {
        super(morphiaCodec);
        needsDocument = LifecycleEvents.needDocument(morphiaCodec.getEntityModel(), morphiaCodec.getDatastore().getMapper(),
                PrePersist.class, PostPersist.class);
    }

    /**
     * @param model  the entity's model
     * @param mapper the mapper
     * @return true if encoding the entity runs callbacks: the model or a configured global listener handles
     *         {@code @PrePersist} or {@code @PostPersist}, or there's any interceptor
     */
    public static boolean isNeeded(EntityModel model, Mapper mapper) {
        return LifecycleEvents.handles(model, mapper, PrePersist.class, PostPersist.class);
    }

    /**
     * @return true if a callback of the entity or a configured global listener takes the {@code Document}, so this always
     *         builds one; otherwise only while there's an interceptor
     */
    boolean needsDocument() {
        return needsDocument;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        EntityModel model = getMorphiaCodec().getEntityModel();
        if (!areEquivalentTypes(value.getClass(), model.getType())) {
            // a subtype: its own codec runs the callbacks, so running them here too would run them twice
            getMorphiaCodec().codecForSubtype((Class) value.getClass())
                    .encode(writer, value, encoderContext);
            return;
        }
        MorphiaDatastore datastore = getMorphiaCodec().getDatastore();

        if (!needsDocument && datastore.getMapper().getListeners().isEmpty()) {
            // no callback looks at the document, so write the entity straight through rather than building one. an
            // interceptor, even one added after this codec was built, always gets the document
            model.callLifecycleMethods(PrePersist.class, value, null, datastore);
            super.encode(writer, value, encoderContext);
            model.callLifecycleMethods(PostPersist.class, value, null, datastore);
            return;
        }

        Document document = new Document();
        model.callLifecycleMethods(PrePersist.class, value, document, datastore);

        final DocumentWriter documentWriter = new DocumentWriter(datastore.getMapper().getConfig(), document);
        super.encode(documentWriter, value, encoderContext);
        document = documentWriter.getDocument();
        model.callLifecycleMethods(PostPersist.class, value, document, datastore);

        getMorphiaCodec().getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

}
