package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static java.lang.String.format;

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

    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeName(name);
        value.encode(datastore, writer, encoderContext);
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public String toString() {
        return format("PipelineField{name='%s', value=%s}", name, value);
    }
}
