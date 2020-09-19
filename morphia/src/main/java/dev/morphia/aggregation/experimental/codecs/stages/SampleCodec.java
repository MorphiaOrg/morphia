package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SampleCodec extends StageCodec<Sample> {
    public SampleCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(BsonWriter writer, Sample value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeInt64("size", value.getSize());
        writer.writeEndDocument();
    }

    @Override
    public Class<Sample> getEncoderClass() {
        return Sample.class;
    }
}
