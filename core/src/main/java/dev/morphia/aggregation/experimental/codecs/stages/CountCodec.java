package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Count;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class CountCodec extends StageCodec<Count> {
    public CountCodec(Datastore datastore) {
        super(datastore);
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
