package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class PipelineField {
    private String name;
    private Expression value;

    PipelineField(final String name, final Expression value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeName(name);
        value.encode(mapper, writer, encoderContext);
    }
}
