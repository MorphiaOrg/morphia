package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class UnsetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;

    public UnsetFieldExpression(Expression field, Object input) {
        super("$unsetField");
        this.field = field;
        this.input = input;
    }

    public Expression field() {
        return field;
    }

    public Object input() {
        return input;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "field", field, encoderContext);
            ExpressionHelper.value(datastore, writer, "input", input, encoderContext);
        });
    }
}
