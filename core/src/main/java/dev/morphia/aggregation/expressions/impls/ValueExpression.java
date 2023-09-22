package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class ValueExpression extends Expression implements SingleValuedExpression {
    private final Object object;

    public ValueExpression(@Nullable Object value) {
        super("unused");
        object = value;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (object != null) {
            Codec codec = datastore.getCodecRegistry().get(object.getClass());
            encoderContext.encodeWithChildContext(codec, writer, object);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public String toString() {
        return String.format("ValueExpression{value=%s}", getValue());
    }
}
