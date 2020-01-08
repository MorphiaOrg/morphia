package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class ArrayExpressions {

    /**
     * Returns the element at the specified array index.
     *
     * @param array the array to use
     * @param index the index to return
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/arrayElemAt $arrayElemAt
     */
    public static Expression elementAt(final Expression array, final Expression index) {
        return new Expression("$arrayElemAt", List.of(array, index)) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
                super.encode(mapper, writer, encoderContext);
            }
        };
    }
}
