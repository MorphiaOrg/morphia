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

class ShortArrayCodec implements Codec<short[]> {

    private Codec<Short> codec;
    private Mapper mapper;

    ShortArrayCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void encode(final BsonWriter writer, final short[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final short cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public Class<short[]> getEncoderClass() {
        return short[].class;
    }

    private Codec<Short> getCodec() {
        if (codec == null) {
            codec = mapper.getCodecRegistry().get(Short.class);
        }
        return codec;
    }

    @Override
    public short[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Short> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        short[] array = new short[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
