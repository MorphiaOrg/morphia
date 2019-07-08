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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ArrayCodec implements Codec<Object[]> {

    private Mapper mapper;
    private BsonTypeCodecMap bsonTypeCodecMap;

    ArrayCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<Object[]> getEncoderClass() {
        return Object[].class;
    }

    private BsonTypeCodecMap getBsonTypeCodecMap() {
        if (bsonTypeCodecMap == null) {
            this.bsonTypeCodecMap = new BsonTypeCodecMap(new BsonTypeClassMap(), mapper.getCodecRegistry());
        }
        return bsonTypeCodecMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(final BsonWriter writer, final Object[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final Object cur : value) {
            final Codec codec = mapper.getCodecRegistry().get(cur.getClass());
            codec.encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public Object[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Object> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(readValue(reader, decoderContext));
        }

        reader.readEndArray();

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
        return getBsonTypeCodecMap().get(bsonType).decode(reader, decoderContext);
    }

}
