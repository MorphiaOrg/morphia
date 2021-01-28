package dev.morphia;

import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Document used to send update statements to the driver.
 *
 * @morphia.internal
 * @since 2.0
 */
public class UpdateDocument implements Bson {
    private final Object entity;

    /**
     * Creates an UpdateDocument for the entity
     *
     * @param entity the entity to update
     * @param <T>    the entity type
     */
    public <T> UpdateDocument(T entity) {
        this.entity = entity;
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {
        DocumentWriter writer = new DocumentWriter();

        MorphiaCodec codec = (MorphiaCodec) codecRegistry.get(entity.getClass());
        codec.encode(writer, entity, EncoderContext.builder().build());

        Document document = writer.getDocument();
        document.remove("_id");
        PropertyModel versionField = codec.getEntityModel().getVersionProperty();
        if (versionField != null) {
            document.remove(versionField.getMappedName());
        }
        return document.toBsonDocument(Document.class, codecRegistry);
    }

}
