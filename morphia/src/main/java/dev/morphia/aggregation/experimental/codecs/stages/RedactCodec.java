package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class RedactCodec extends StageCodec<Redact> {
    public RedactCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Redact> getEncoderClass() {
        return Redact.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Redact value, final EncoderContext encoderContext) {
       value.getExpression().encode(getMapper(), writer, encoderContext);
    }
}
