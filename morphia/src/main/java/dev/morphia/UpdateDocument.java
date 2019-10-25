package dev.morphia;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class UpdateDocument implements Bson {


    public enum Mode {
        DEFAULT, BODY_ONLY;
    }
    private Mapper mapper;

    private Object entity;
    private Mode mode;
    private boolean skipVersion;

    public <T> UpdateDocument(final Mapper mapper, final T entity, final Mode mode) {
        this.mapper = mapper;
        this.entity = entity;
        this.mode = mode;
    }

    public void skipVersion() {
        skipVersion = true;
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
        return mode == Mode.DEFAULT
               ? new Document("$set", document).toBsonDocument(Document.class, codecRegistry)
               : document.toBsonDocument(Document.class, codecRegistry);
    }
}
