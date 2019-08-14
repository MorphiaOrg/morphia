package dev.morphia;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class UpdateDocument implements Bson {
    private boolean skipVersion;

    public void skipVersion() {
        skipVersion = true;
    }

    public enum Mode {
        DEFAULT, BODY_ONLY
    }
    private Object entity;
    private Mode mode;

    public <T> UpdateDocument(final T entity, final Mode mode) {
        this.entity = entity;
        this.mode = mode;
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> tDocumentClass, final CodecRegistry codecRegistry) {
        DocumentWriter writer = new DocumentWriter();

        MorphiaCodec codec = (MorphiaCodec) codecRegistry.get(entity.getClass());
        codec.encode(writer, entity, EncoderContext.builder().build());

        Document document = writer.getRoot();
        document.remove("_id");
        MappedField versionField = codec.getMappedClass().getVersionField();
        if (versionField != null /*&& !versionField.getFieldValue(entity).equals(1L)*/) {
            document.remove(versionField.getMappedFieldName());
        }
        return mode == Mode.DEFAULT
               ? new Document("$set", document).toBsonDocument(Document.class, codecRegistry)
               : document.toBsonDocument(Document.class, codecRegistry);
    }
}
