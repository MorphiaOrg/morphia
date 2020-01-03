package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.expressions.Expression.writeNamedExpression;

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
        Fields<Bucket> output = value.getOutput();
        if (output != null) {
            writer.writeStartDocument("output");
            output.encode(getMapper(), writer, encoderContext);
            writer.writeEndDocument();
        }
    }

    private void writeNamedValue(final BsonWriter writer, final String name, final Object value, final EncoderContext encoderContext) {
        if (value != null) {
            writer.writeName(name);
            Codec codec = getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }
}
