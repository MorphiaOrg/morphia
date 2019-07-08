package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

class LongArrayCodec implements Codec<long[]> {

    private Codec<Long> codec;
    private Mapper mapper;

    LongArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<long[]> getEncoderClass() {
        return long[].class;
    }

    private Codec<Long> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(Long.class);
        }
        return codec;
    }
    
    @Override
    public void encode(final BsonWriter writer, final long[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final long cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public long[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Long> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        long[] array = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
