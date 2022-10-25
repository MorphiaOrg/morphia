package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

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

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            writer.writeName("input");
            expression(datastore, writer, input, encoderContext);
            if (n != null) {
                value(writer, "N", n);
            } else {
                value(datastore, writer, "alpha", alpha, encoderContext);
            }
        });
    }
}
