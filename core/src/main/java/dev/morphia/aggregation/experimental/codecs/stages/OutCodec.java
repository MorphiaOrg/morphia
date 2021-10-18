package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Out;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class OutCodec extends StageCodec<Out> {
    public OutCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Out> getEncoderClass() {
        return Out.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encodeStage(BsonWriter writer, Out value, EncoderContext encoderContext) {
        Class<?> type = value.getType();
        if (type != null) {
            writer.writeString(getDatastore().getMapper().getEntityModel(type).getCollectionName());
        } else {
            writer.writeString(value.getCollection());
        }
    }
}
