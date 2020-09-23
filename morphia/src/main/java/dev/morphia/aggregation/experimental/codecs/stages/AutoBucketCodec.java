package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class AutoBucketCodec extends StageCodec<AutoBucket> {
    public AutoBucketCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class getEncoderClass() {
        return AutoBucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, AutoBucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            expression(getMapper(), writer, "groupBy", value.getGroupBy(), encoderContext);
            value(getMapper(), writer, "buckets", value.getBuckets(), encoderContext);
            value(getMapper(), writer, "granularity", value.getGranularity(), encoderContext);
            DocumentExpression output = value.getOutput();
            if (output != null) {
                output.encode("output", getMapper(), writer, encoderContext);
            }
        });
    }
}
