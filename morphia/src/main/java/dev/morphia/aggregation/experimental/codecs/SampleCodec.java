package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Sample;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SampleCodec extends StageCodec<Sample> {
    @Override
    protected void encodeStage(final BsonWriter writer, final Sample value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeInt32("size", value.getSize());
        writer.writeEndDocument();
    }

    @Override
    public Class<Sample> getEncoderClass() {
        return Sample.class;
    }
}
