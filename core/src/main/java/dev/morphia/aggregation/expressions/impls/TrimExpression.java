package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class TrimExpression extends Expression {
    private final Expression input;
    private Expression chars;

    public TrimExpression(String operator, Expression input) {
        super(operator);
        this.input = input;
    }

    public TrimExpression chars(Expression chars) {
        this.chars = chars;
        return this;
    }

    public Expression input() {
        return input;
    }

    public Expression chars() {
        return chars;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }
}
