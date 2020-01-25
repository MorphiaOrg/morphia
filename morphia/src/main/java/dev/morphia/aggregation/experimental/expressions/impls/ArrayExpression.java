package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Base class for the array expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#array-expression-operators Array Expressions
 */
public class ArrayExpression extends Expression {

    public ArrayExpression(final String operation, final Object value) {
        super(operation, value);
    }


    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        super.encode(mapper, writer, encoderContext);
    }

}
