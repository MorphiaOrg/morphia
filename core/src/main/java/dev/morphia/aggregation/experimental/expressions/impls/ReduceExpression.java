package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.wrapExpression;

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
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, "input", input, encoderContext);
            wrapExpression(datastore, writer, "initialValue", initial, encoderContext);
            wrapExpression(datastore, writer, "in", in, encoderContext);
        });
    }
}
