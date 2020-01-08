package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class ArrayExpressions {

    public static Expression elementAt(final Expression array, final Expression index) {
        return new Expression("$arrayElemAt", List.of(array, index)) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
                super.encode(mapper, writer, encoderContext);
            }
        };
    }
}
