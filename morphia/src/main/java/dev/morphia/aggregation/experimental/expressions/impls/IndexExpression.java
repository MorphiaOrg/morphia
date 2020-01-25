package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedExpression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedValue;
import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedValue;

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
    public IndexExpression(final String operation, final Expression string, final Expression substring) {
        super(operation);
        this.string = string;
        this.substring = substring;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        writeUnnamedExpression(mapper, writer, string, encoderContext);
        writeUnnamedExpression(mapper, writer, substring, encoderContext);
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
    public IndexExpression end(final int end) {
        this.end = end;
        return this;
    }

    /**
     * Sets the start boundary for searching
     *
     * @param start the start
     * @return this
     */
    public IndexExpression start(final int start) {
        this.start = start;
        return this;
    }
}
