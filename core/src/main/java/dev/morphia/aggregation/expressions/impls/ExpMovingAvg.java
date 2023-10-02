package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class ExpMovingAvg extends Expression {
    private final Expression input;
    private final Integer n;
    private final Double alpha;

    public ExpMovingAvg(Expression input, int n) {
        super("$expMovingAvg");
        this.input = input;
        this.n = n;
        alpha = null;
    }

    public ExpMovingAvg(Expression input, double alpha) {
        super("$expMovingAvg");
        this.input = input;
        this.n = null;
        this.alpha = alpha;
    }

    public Expression input() {
        return input;
    }

    @Nullable
    public Integer n() {
        return n;
    }

    public Double alpha() {
        return alpha;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }
}
