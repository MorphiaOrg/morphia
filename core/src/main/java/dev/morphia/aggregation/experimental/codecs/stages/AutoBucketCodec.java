package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class AutoBucketCodec extends StageCodec<AutoBucket> {
    public AutoBucketCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class getEncoderClass() {
        return AutoBucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, AutoBucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            expression(getDatastore(), writer, "groupBy", value.getGroupBy(), encoderContext);
            value(getDatastore(), writer, "buckets", value.getBuckets(), encoderContext);
            value(getDatastore(), writer, "granularity", value.getGranularity(), encoderContext);
            DocumentExpression output = value.getOutput();
            if (output != null) {
                output.encode("output", getDatastore(), writer, encoderContext);
            }
        });
    }
}
