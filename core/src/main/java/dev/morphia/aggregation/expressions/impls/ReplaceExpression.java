package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

/**
 * Defines expressions for $replaceAll and $replaceOne
 *
 * @since 2.1
 */
public class ReplaceExpression extends Expression {
    private final Expression find;
    private final Expression replacement;
    private final Expression input;

    /**
     * @param operator    the operator name
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @morphia.internal
     */
    public ReplaceExpression(String operator,
                             Expression input,
                             Expression find,
                             Expression replacement) {
        super(operator);
        this.input = input;
        this.find = find;
        this.replacement = replacement;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, "input", input, encoderContext);
            wrapExpression(datastore, writer, "find", find, encoderContext);
            wrapExpression(datastore, writer, "replacement", replacement, encoderContext);
        });
    }
}
