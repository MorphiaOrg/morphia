package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class SkipCodec extends StageCodec<Skip> {
    public SkipCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Skip value, final EncoderContext encoderContext) {
        writer.writeInt64(value.getSize());
    }

    @Override
    public Class<Skip> getEncoderClass() {
        return Skip.class;
    }
}
