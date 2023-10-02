package dev.morphia.aggregation.expressions.impls;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.aggregation.expressions.Expressions.field;

@SuppressWarnings("unchecked")
public class Fields {
    private final List<PipelineField> fields = new ArrayList<>();
    private Object owner;

    protected Fields(Object owner) {
        this.owner = owner;
    }

    private Fields() {
    }

    public static Fields on(Object owner) {
        return new Fields(owner);
    }

    public Object add(String name) {
        fields.add(new PipelineField(name, field(name)));
        return owner;
    }

    public <T> T add(String name, Expression expression) {
        fields.add(new PipelineField(name, expression));
        return (T) owner;
    }

    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (1 == 1)
            throw new UnsupportedOperationException();
        for (PipelineField field : fields) {
            wrapExpression(datastore, writer, field.getName(), field.getValue(), encoderContext);
        }
    }

    public List<PipelineField> getFields() {
        return fields;
    }

    public int size() {
        return fields.size();
    }

}
