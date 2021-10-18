package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private String as;

    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                expression(datastore, writer, "input", array, encoderContext);
                expression(datastore, writer, "cond", conditional, encoderContext);
                value(datastore, writer, "as", as, encoderContext);
            });
        });
    }
}
