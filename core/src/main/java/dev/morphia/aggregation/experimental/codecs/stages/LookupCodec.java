package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Lookup;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class LookupCodec extends StageCodec<Lookup> {

    public LookupCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Lookup> getEncoderClass() {
        return Lookup.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Lookup value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getFrom() != null) {
                value(getDatastore(), writer, "from", value.getFrom(), encoderContext);
            } else {
                writer.writeString("from", getDatastore().getMapper().getEntityModel(value.getFromType()).getCollectionName());
            }

            if (value.getPipeline() == null) {
                writer.writeString("localField", value.getLocalField());
                writer.writeString("foreignField", value.getForeignField());
            } else {
                value(getDatastore(), writer, "let", value.getVariables(), encoderContext);
                value(getDatastore(), writer, "pipeline", value.getPipeline(), encoderContext);
            }
            writer.writeString("as", value.getAs());
        });
    }
}
