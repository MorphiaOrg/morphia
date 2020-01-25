package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedValue;

/**
 * @since 2.o
 */
public class ArrayIndexExpression extends Expression {
    private final Expression array;
    private final Expression search;
    private Integer start;
    private Integer end;

    /**
     * @param array
     * @param search
     * @morphia.internal
     */
    public ArrayIndexExpression(final Expression array, final Expression search) {
        super("$indexOfArray");
        this.array = array;
        this.search = search;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        writeUnnamedExpression(mapper, writer, array, encoderContext);
        writeUnnamedExpression(mapper, writer, search, encoderContext);
        writeUnnamedValue(mapper, writer, start, encoderContext);
        writeUnnamedValue(mapper, writer, end, encoderContext);
        writer.writeEndArray();
        writer.writeEndDocument();

    }

    /**
     * The ending index
     *
     * @param end the ending index
     * @return this
     */
    public ArrayIndexExpression end(final Integer end) {
        this.end = end;
        return this;
    }

    /**
     * The starting index
     *
     * @param start the starting index
     * @return this
     */
    public ArrayIndexExpression start(final Integer start) {
        this.start = start;
        return this;
    }
}
