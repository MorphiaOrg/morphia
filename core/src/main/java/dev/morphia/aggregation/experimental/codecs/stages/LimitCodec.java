package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Limit;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LimitCodec extends StageCodec<Limit> {
    public LimitCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public void encodeStage(BsonWriter writer, Limit value, EncoderContext encoderContext) {
        writer.writeInt64(value.getLimit());
    }

    @Override
    public Class<Limit> getEncoderClass() {
        return Limit.class;
    }
}
