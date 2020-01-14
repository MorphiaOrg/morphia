package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.internal.DocumentExpression;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedExpression;

public class BucketCodec extends StageCodec<Bucket> {
    public BucketCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class getEncoderClass() {
        return Bucket.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final Bucket value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeNamedExpression(getMapper(), writer, "groupBy", value.getGroupBy(), encoderContext);
        writeNamedValue(writer, "boundaries", value.getBoundaries(), encoderContext);
        writeNamedValue(writer, "default", value.getDefaultValue(), encoderContext);
        DocumentExpression output = value.getOutput();
        if (output != null) {
            output.encode("output", getMapper(), writer, encoderContext);
        }
        writer.writeEndDocument();
    }

}
