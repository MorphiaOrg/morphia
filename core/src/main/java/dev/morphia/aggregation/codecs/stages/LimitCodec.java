package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Limit;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LimitCodec extends StageCodec<Limit> {
    public LimitCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encodeStage(BsonWriter writer, Limit value, EncoderContext encoderContext) {
        writer.writeInt64(value.limit());
    }

    @Override
    public Class<Limit> getEncoderClass() {
        return Limit.class;
    }
}
