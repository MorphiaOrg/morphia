package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.stages.Bucket;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class BucketCodec extends StageCodec<Bucket> {
    @Override
    public Class getEncoderClass() {
        return Bucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Bucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            expression(getDatastore(), writer, "groupBy", value.getGroupBy(), encoderContext);
            expression(getDatastore(), writer, "boundaries", value.getBoundaries(), encoderContext);
            value(getDatastore(), writer, "default", value.getDefaultValue(), encoderContext);
            DocumentExpression output = value.getOutput();
            if (output != null) {
                output.encode("output", getDatastore(), writer, encoderContext);
            }
        });
    }
}
