package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Limit;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LimitCodec extends StageCodec<Limit> {
    @Override
    public void encodeStage(final BsonWriter writer, final Limit value, final EncoderContext encoderContext) {
        writer.writeInt32(value.getLimit());
    }

    @Override
    public Class<Limit> getEncoderClass() {
        return Limit.class;
    }
}
