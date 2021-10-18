package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static java.util.Arrays.asList;

public class ArrayLiteral extends ArrayExpression {
    private final List<Expression> values;

    public ArrayLiteral(Expression... values) {
        super("unused", null);
        this.values = asList(values);
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        Codec codec = datastore.getCodecRegistry().get(values.getClass());
        encoderContext.encodeWithChildContext(codec, writer, values);
    }
}
