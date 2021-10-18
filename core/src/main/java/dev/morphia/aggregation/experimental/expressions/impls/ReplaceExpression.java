package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

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
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                value(datastore, writer, "input", input, encoderContext);
                value(datastore, writer, "find", find, encoderContext);
                value(datastore, writer, "replacement", replacement, encoderContext);
            });
        });
    }
}
