package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class ReduceExpression extends Expression {
    private final Expression input;
    private final Expression initial;
    private final Expression in;

    public ReduceExpression(Expression input, Expression initial, Expression in) {
        super("$reduce");
        this.input = input;
        this.initial = initial;
        this.in = in;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, "input", input, encoderContext);
            wrapExpression(datastore, writer, "initialValue", initial, encoderContext);
            wrapExpression(datastore, writer, "in", in, encoderContext);
        });
    }
}
