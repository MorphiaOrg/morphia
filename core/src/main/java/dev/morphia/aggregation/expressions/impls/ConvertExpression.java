package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class ConvertExpression extends Expression {
    private final Expression input;
    private final ConvertType to;
    private Expression onError;
    private Expression onNull;

    public ConvertExpression(Expression input, ConvertType to) {
        super("$convert");
        this.input = input;
        this.to = to;
    }

    public Expression input() {
        return input;
    }

    public ConvertType to() {
        return to;
    }

    public Expression onError() {
        return onError;
    }

    public Expression onNull() {
        return onNull;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    /**
     * The value to return on encountering an error during conversion, including unsupported type conversions.
     *
     * @param onError the value
     * @return this
     */
    public ConvertExpression onError(Expression onError) {
        this.onError = onError;
        return this;
    }

    /**
     * The value to return if the input is null or missing.
     *
     * @param onNull the value
     * @return this
     */
    public ConvertExpression onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }
}
