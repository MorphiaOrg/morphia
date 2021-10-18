package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import org.bson.BsonBinarySubType;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ArrayCodec implements Codec<Object> {

    private final Class type;
    private final Datastore datastore;

    <T> ArrayCodec(Datastore datastore, Class type) {
        this.datastore = datastore;
        this.type = type;
    }

    @Override
    public Object[] decode(BsonReader reader, DecoderContext decoderContext) {
        List<Object> list = new ArrayList<>();
        if (reader.getCurrentBsonType() == BsonType.ARRAY) {
            reader.readStartArray();

            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                list.add(readValue(reader, decoderContext));
            }

            reader.readEndArray();
        } else {
            list.add(readValue(reader, decoderContext));
        }

        return list.toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        writer.writeStartArray();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            if (element == null) {
                writer.writeNull();
            } else {
                Codec codec = datastore.getCodecRegistry().get(element.getClass());
                codec.encode(writer, element, encoderContext);
            }
        }
        writer.writeEndArray();
    }

    @Override
    public Class<Object> getEncoderClass() {
        return null;
    }

    @Nullable
    private Object readValue(BsonReader reader, DecoderContext decoderContext) {
        BsonType bsonType = reader.getCurrentBsonType();
        if (bsonType == BsonType.NULL) {
            reader.readNull();
            return null;
        } else if (bsonType == BsonType.BINARY && BsonBinarySubType.isUuid(reader.peekBinarySubType()) && reader.peekBinarySize() == 16) {
            return datastore.getCodecRegistry().get(UUID.class).decode(reader, decoderContext);
        }
        return datastore.getCodecRegistry().get(type.getComponentType()).decode(reader, decoderContext);
    }

}
