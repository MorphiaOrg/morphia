package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SkipCodec extends StageCodec<Skip> {
    public SkipCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Skip> getEncoderClass() {
        return Skip.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Skip value, EncoderContext encoderContext) {
        writer.writeInt64(value.getSize());
    }
}
