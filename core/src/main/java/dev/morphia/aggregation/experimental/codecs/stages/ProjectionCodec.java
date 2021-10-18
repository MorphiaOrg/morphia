package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.PipelineField;
import dev.morphia.aggregation.experimental.stages.Projection;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class ProjectionCodec extends StageCodec<Projection> {

    public ProjectionCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Projection> getEncoderClass() {
        return Projection.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Projection projection, EncoderContext encoderContext) {
        document(writer, () -> {
            for (PipelineField field : projection.getFields()) {
                value(getDatastore(), writer, field.getName(), field.getValue(), encoderContext);
            }
        });
    }
}
