package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeUnnamedValue;

/**
 * @since 2.0
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
    public ArrayIndexExpression(Expression array, Expression search) {
        super("$indexOfArray");
        this.array = array;
        this.search = search;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        expression(mapper, writer, array, encoderContext);
        expression(mapper, writer, search, encoderContext);
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
    public ArrayIndexExpression end(Integer end) {
        this.end = end;
        return this;
    }

    /**
     * The starting index
     *
     * @param start the starting index
     * @return this
     */
    public ArrayIndexExpression start(Integer start) {
        this.start = start;
        return this;
    }
}
