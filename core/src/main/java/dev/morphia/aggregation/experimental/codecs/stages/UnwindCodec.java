package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Unwind;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class UnwindCodec extends StageCodec<Unwind> {
    public UnwindCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Unwind> getEncoderClass() {
        return Unwind.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Unwind value, EncoderContext encoderContext) {
        if (!value.optionsPresent()) {
            value.getPath().encode(getDatastore(), writer, encoderContext);
        } else {
            document(writer, () -> {
                expression(getDatastore(), writer, "path", value.getPath(), encoderContext);
                value(getDatastore(), writer, "includeArrayIndex", value.getIncludeArrayIndex(), encoderContext);
                value(getDatastore(), writer, "preserveNullAndEmptyArrays", value.getPreserveNullAndEmptyArrays(), encoderContext);
            });
        }
    }
}
