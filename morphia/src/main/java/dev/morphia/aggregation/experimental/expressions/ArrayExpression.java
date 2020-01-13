package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.internal.ArrayLiteral;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class ArrayExpression extends Expression {

    protected ArrayExpression(final String operation, final Object value) {
        super(operation, value);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        super.encode(mapper, writer, encoderContext);
    }

    public static ArrayExpression array(final Expression... expressions) {
        return new ArrayLiteral(expressions);
    }

    /**
     * Returns the element at the specified array index.
     *
     * @param array the array to use
     * @param index the index to return
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/arrayElemAt $arrayElemAt
     */
    public static ArrayExpression elementAt(final Expression array, final Expression index) {
        return new ArrayExpression("$arrayElemAt", List.of(array, index));
    }
    /**
     * Counts and returns the total number of items in an array.
     *
     * @param array the array to use
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/size $size
     */
    public static ArrayExpression size(final Expression array) {
        return new ArrayExpression("$size", array);
    }

}
