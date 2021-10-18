package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * Defines the $indexOfBytes expression
 *
 * @morphia.internal
 * @since 2.0
 */
public class IndexExpression extends Expression {
    private final Expression string;
    private final Expression substring;
    private Integer end;
    private Integer start;

    /**
     * Creates the new expression
     *
     * @param operation the index operation name
     * @param string    the string to search
     * @param substring the target string
     */
    public IndexExpression(String operation, Expression string, Expression substring) {
        super(operation);
        this.string = string;
        this.substring = substring;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            array(writer, getOperation(), () -> {
                expression(datastore, writer, string, encoderContext);
                expression(datastore, writer, substring, encoderContext);
                value(datastore, writer, start, encoderContext);
                value(datastore, writer, end, encoderContext);
            });
        });
    }

    /**
     * Sets the end boundary for searching
     *
     * @param end the end
     * @return this
     */
    public IndexExpression end(int end) {
        this.end = end;
        return this;
    }

    /**
     * Sets the start boundary for searching
     *
     * @param start the start
     * @return this
     */
    public IndexExpression start(int start) {
        this.start = start;
        return this;
    }
}
