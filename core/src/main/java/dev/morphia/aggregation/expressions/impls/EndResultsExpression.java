package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Gives first/last results
 */
public class EndResultsExpression extends Expression {
    private final Expression input;
    private final Expression n;

    public EndResultsExpression(String operation, Expression n, Expression input) {
        super(operation);
        this.input = input;
        this.n = n;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "input", input, encoderContext);
            expression(datastore, writer, "n", n, encoderContext);
        });
    }
}
