package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.Skip;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SkipCodec extends StageCodec<Skip> {
    @Override
    public Class<Skip> getEncoderClass() {
        return Skip.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Skip value, EncoderContext encoderContext) {
        writer.writeInt64(value.getSize());
    }
}
