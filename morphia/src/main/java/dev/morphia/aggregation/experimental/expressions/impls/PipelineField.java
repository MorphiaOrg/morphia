package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 */
public class PipelineField {
    private final String name;
    private final Expression value;

    public PipelineField(String name, Expression value) {
        this.name = name;
        this.value = value;
    }

    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeName(name);
        value.encode(mapper, writer, encoderContext);
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }
}
