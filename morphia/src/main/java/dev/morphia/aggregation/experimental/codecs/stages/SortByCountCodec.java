package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SortByCountCodec extends StageCodec<SortByCount> {
    public SortByCountCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<SortByCount> getEncoderClass() {
        return SortByCount.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, SortByCount value, EncoderContext encoderContext) {
        value.getExpression().encode(getMapper(), writer, encoderContext);
    }
}
