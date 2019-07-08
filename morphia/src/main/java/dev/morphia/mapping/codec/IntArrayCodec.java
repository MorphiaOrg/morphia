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

class IntArrayCodec implements Codec<int[]> {


    private Codec<Integer> codec;
    private Mapper mapper;

    IntArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<int[]> getEncoderClass() {
        return int[].class;
    }

    private Codec<Integer> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(Integer.class);
        }
        return codec;
    }
    
    @Override
    public void encode(final BsonWriter writer, final int[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final int cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public int[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Integer> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
