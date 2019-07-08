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

class BooleanArrayCodec implements Codec<boolean[]> {

    private Codec<Boolean> codec;
    private Mapper mapper;

    BooleanArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    private Codec<Boolean> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(Boolean.class);
        }
        return codec;
    }

    @Override
    public Class<boolean[]> getEncoderClass() {
        return boolean[].class;
    }

    @Override
    public void encode(final BsonWriter writer, final boolean[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final boolean cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public boolean[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Boolean> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        boolean[] array = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
