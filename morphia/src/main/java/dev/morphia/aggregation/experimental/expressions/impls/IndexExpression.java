package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeUnnamedValue;

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
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        expression(mapper, writer, string, encoderContext);
        expression(mapper, writer, substring, encoderContext);
        writeUnnamedValue(mapper, writer, start, encoderContext);
        writeUnnamedValue(mapper, writer, end, encoderContext);
        writer.writeEndArray();
        writer.writeEndDocument();
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
