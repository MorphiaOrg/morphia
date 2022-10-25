package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private ValueExpression as;

    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(String as) {
        this.as = new ValueExpression(as);
        return this;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, "input", array, encoderContext);
            wrapExpression(datastore, writer, "cond", conditional, encoderContext);
            ExpressionHelper.expression(datastore, writer, "as", as, encoderContext);
        });
    }
}
