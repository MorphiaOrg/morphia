package dev.morphia;

import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class UpdateDocument implements Bson {
    private Object entity;

    public <T> UpdateDocument(final T entity) {
        this.entity = entity;
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> tDocumentClass, final CodecRegistry codecRegistry) {
        DocumentWriter writer = new DocumentWriter();

        Codec codec = codecRegistry.get(entity.getClass());
        codec.encode(writer, entity, EncoderContext.builder().build());

        Document document = writer.getRoot();
        document.remove("_id");
        return new Document("$set", document).toBsonDocument(Document.class, codecRegistry);
    }
}
