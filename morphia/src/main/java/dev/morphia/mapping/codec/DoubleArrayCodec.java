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

class DoubleArrayCodec implements Codec<double[]> {

    private Codec<Double> codec;
    private Mapper mapper;

    DoubleArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<double[]> getEncoderClass() {
        return double[].class;
    }
  
    private Codec<Double> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(Double.class);
        }
        return codec;
    }

    @Override
    public void encode(final BsonWriter writer, final double[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final double cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public double[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<Double> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
