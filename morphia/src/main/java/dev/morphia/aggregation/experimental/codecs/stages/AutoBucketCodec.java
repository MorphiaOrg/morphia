package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.expressions.Expression.writeNamedExpression;

public class AutoBucketCodec extends StageCodec<AutoBucket> {
    public AutoBucketCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class getEncoderClass() {
        return AutoBucket.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final AutoBucket value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeNamedExpression(getMapper(), writer, "groupBy", value.getGroupBy(), encoderContext);
        writeNamedValue(writer, "buckets", value.getBuckets(), encoderContext);
        writeNamedValue(writer, "granularity", value.getGranularity(), encoderContext);
        Fields<AutoBucket> output = value.getOutput();
        if (output != null) {
            writer.writeStartDocument("output");
            output.encode(getMapper(), writer, encoderContext);
            writer.writeEndDocument();
        }
    }
}
