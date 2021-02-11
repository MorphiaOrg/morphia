package dev.morphia.aggregation.experimental.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Base class for the array expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#array-expression-operators Array Expressions
 */
public class ArrayExpression extends Expression {

    public ArrayExpression(String operation, @Nullable Object value) {
        super(operation, value);
    }


    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        super.encode(mapper, writer, encoderContext);
    }

}
