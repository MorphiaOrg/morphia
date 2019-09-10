package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.BsonBinarySubType;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.BsonTypeCodecMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ArrayCodec implements Codec<Object> {

    private Mapper mapper;
    private final Class type;
    private BsonTypeCodecMap bsonTypeCodecMap;

    public <T> ArrayCodec(final Mapper mapper, final Class type) {
        this.mapper = mapper;
        this.type = type;
    }

    @Override
    public Class<Object> getEncoderClass() {
        return null;
    }

    private BsonTypeCodecMap getBsonTypeCodecMap() {
        if (bsonTypeCodecMap == null) {
            this.bsonTypeCodecMap = new BsonTypeCodecMap(new BsonTypeClassMap(), mapper.getCodecRegistry());
        }
        return bsonTypeCodecMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
        try {
            writer.writeStartArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                Codec codec = mapper.getCodecRegistry().get(element.getClass());
                codec.encode(writer, element, encoderContext);
            }
            writer.writeEndArray();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        List<Object> list = new ArrayList<>();
        if(reader.getCurrentBsonType() == BsonType.ARRAY) {
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

    private Object readValue(final BsonReader reader, final DecoderContext decoderContext) {
        BsonType bsonType = reader.getCurrentBsonType();
        if (bsonType == BsonType.NULL) {
            reader.readNull();
            return null;
        } else if (bsonType == BsonType.BINARY && BsonBinarySubType.isUuid(reader.peekBinarySubType()) && reader.peekBinarySize() == 16) {
            return mapper.getCodecRegistry().get(UUID.class).decode(reader, decoderContext);
        }
        return mapper.getCodecRegistry().get(type.getComponentType()).decode(reader, decoderContext);
    }

}
