package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Sample;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class SampleCodec implements Codec<Sample> {
    @Override
    public Sample decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Sample value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("$sample");
        writer.writeStartDocument();
        writer.writeInt32("size", value.getSize());
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    @Override
    public Class<Sample> getEncoderClass() {
        return Sample.class;
    }
}
