package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class CountCodec extends StageCodec<Count> {
    public CountCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Count> getEncoderClass() {
        return Count.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Count value, EncoderContext encoderContext) {
        writer.writeString(value.getName());
    }
}
