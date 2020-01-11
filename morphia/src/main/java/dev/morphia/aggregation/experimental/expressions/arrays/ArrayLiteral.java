package dev.morphia.aggregation.experimental.expressions.arrays;

import dev.morphia.aggregation.experimental.expressions.ArrayExpression;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static java.util.Arrays.asList;

public class ArrayLiteral extends ArrayExpression {
    private final List<Expression> values;

    public ArrayLiteral(final Expression... values) {
        super(null, null);
        this.values = asList(values);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        Codec codec = mapper.getCodecRegistry().get(values.getClass());
        encoderContext.encodeWithChildContext(codec, writer, values);
    }
}
