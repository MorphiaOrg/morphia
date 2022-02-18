package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static java.util.Arrays.asList;

public class ArrayLiteral extends ArrayExpression {

    public ArrayLiteral(Expression... values) {
        super("unused", asList(values));
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        Expression value = getValue();
        if (value != null) {
            value.encode(datastore, writer, encoderContext);
        }
    }
}
