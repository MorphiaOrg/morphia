package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.SortByCount;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SortByCountCodec extends StageCodec<SortByCount> {
    @Override
    public Class<SortByCount> getEncoderClass() {
        return SortByCount.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, SortByCount value, EncoderContext encoderContext) {
        value.getExpression().encode(getDatastore(), writer, encoderContext);
    }
}
