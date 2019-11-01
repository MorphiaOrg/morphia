package dev.morphia;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

/**
 * Document used to send update statements to the driver.
 *
 * @morphia.internal
 */
public class UpdateDocument implements Bson {
    private Object entity;

    /**
     * Creates an UpdateDocument for the entity
     *
     * @param entity the entity to update
     * @param <T>    the entity type
     */
    public <T> UpdateDocument(final T entity) {
        this.entity = entity;
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> tDocumentClass, final CodecRegistry codecRegistry) {
        DocumentWriter writer = new DocumentWriter();

        MorphiaCodec codec = (MorphiaCodec) codecRegistry.get(entity.getClass());
        codec.encode(writer, entity, EncoderContext.builder().build());

        Document document = writer.getRoot();
        document.remove("_id");
        MappedField versionField = codec.getMappedClass().getVersionField();
        if (versionField != null) {
            document.remove(versionField.getMappedFieldName());
        }
        return document.toBsonDocument(Document.class, codecRegistry);
    }

}
