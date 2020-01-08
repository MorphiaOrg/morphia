package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedExpression;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;

public class Fields<T> {
    private T owner;
    private final List<PipelineField> fields = new ArrayList<>();

    protected Fields(final T owner) {
        this.owner = owner;
    }

    private Fields() {
    }

    public static <T> Fields<T> on(final T owner) {
        return new Fields<>(owner);
    }

    public T add(final String name) {
        fields.add(new PipelineField(name, field(name)));
        return owner;
    }

    public T add(final String name, final Expression expression) {
        fields.add(new PipelineField(name, expression));
        return owner;
    }

    public List<PipelineField> getFields() {
        return fields;
    }

    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        for (final PipelineField field : fields) {
            writeNamedExpression(mapper, writer, field.getName(), field.getValue(), encoderContext);
        }
    }

    public int size() {
        return fields.size();
    }

}
