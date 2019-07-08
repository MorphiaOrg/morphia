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

class FloatArrayCodec implements Codec<float[]> {

    private Codec<Float> codec;
    private Mapper mapper;

    FloatArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<float[]> getEncoderClass() {
        return float[].class;
    }

    private Codec<Float> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(Float.class);
        }
        return codec;
    }
    
    @Override
    public void encode(final BsonWriter writer, final float[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final float cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public float[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Float> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
