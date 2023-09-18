package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.stages.AutoBucket;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

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
            ExpressionHelper.expression(getDatastore(), writer, "buckets", value.getBuckets(), encoderContext);
            ExpressionHelper.expression(getDatastore(), writer, "granularity", value.getGranularity(), encoderContext);
            DocumentExpression output = value.getOutput();
            if (output != null) {
                output.encode("output", getDatastore(), writer, encoderContext);
            }
        });
    }
}
