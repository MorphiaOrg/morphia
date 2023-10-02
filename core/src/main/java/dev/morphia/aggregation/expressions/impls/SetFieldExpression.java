package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class SetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;
    private final Expression value;

    public SetFieldExpression(Expression field, Object input, Expression value) {
        super("$setField");
        this.field = field;
        this.input = input;
        this.value = value;
    }

    public Expression field() {
        return field;
    }

    public Object input() {
        return input;
    }

    @Override
    public Expression value() {
        return value;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }
}
