package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.Unwind;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class UnwindCodec extends StageCodec<Unwind> {
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
                value(writer, "includeArrayIndex", value.getIncludeArrayIndex());
                value(writer, "preserveNullAndEmptyArrays", value.getPreserveNullAndEmptyArrays());
            });
        }
    }
}
