package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Out;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

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
        Class<?> type = value.type();
        String collectionName;
        if (type != null) {
            collectionName = getDatastore().getMapper().getEntityModel(type).getCollectionName();
        } else {
            collectionName = value.collection();
        }
        if (value.database() == null) {
            writer.writeString(collectionName);
        } else {
            document(writer, () -> {
                writer.writeString("db", value.database());
                writer.writeString("coll", collectionName);
            });
        }
    }
}
