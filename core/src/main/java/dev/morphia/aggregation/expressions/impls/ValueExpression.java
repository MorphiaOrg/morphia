package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ValueExpression extends Expression implements SingleValuedExpression {
    private final Object object;

    public ValueExpression(@Nullable Object value) {
        super("unused");
        object = value;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public Object object() {
        return object;
    }

    @Override
    public String toString() {
        return "ValueExpression{value=%s}".formatted(object);
    }
}
