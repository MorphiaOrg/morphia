package dev.morphia.mapping.codec;

import com.mongodb.DBRef;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyModel;

public class ReferenceCodec {

    private Mapper mapper;
    private final PropertyModel propertyModel;
    private final MappedField field;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private final MappedField idField;
    private final MappedClass mappedClass;

    public ReferenceCodec(final Mapper mapper, final PropertyModel propertyModel, final MappedField field) {
        this.mapper = mapper;
        this.propertyModel = propertyModel;
        this.field = field;
        final MorphiaCodec codec = (MorphiaCodec) mapper.getCodecRegistry().get(field.getType());
        mappedClass = codec.getMappedClass();

        idField = mappedClass.getIdField();
    }

    public <S> S decode(final BsonReader reader, final DecoderContext decoderContext) {
        return (S) mapper.getCodecRegistry()
                         .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                         .decode(reader, decoderContext);
    }

    public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
        if (value == null) {
            writer.writeNull(propertyModel.getReadName());
        } else {
            writer.writeName(propertyModel.getReadName());
            Object idValue = idField.getFieldValue(value);
            if(!field.getAnnotation(Reference.class).idOnly()) {
                idValue = new DBRef(mappedClass.getCollectionName(), idValue);
            }

            final Codec codec = mapper.getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
        }
    }
}
