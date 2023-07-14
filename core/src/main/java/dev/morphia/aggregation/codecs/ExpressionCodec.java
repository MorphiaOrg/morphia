package dev.morphia.aggregation.codecs;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.internal.DatastoreHolder;
import dev.morphia.sofia.Sofia;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ExpressionCodec<T extends Expression> implements Codec<T> {

    public ExpressionCodec() {
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(BsonWriter writer, T expression, EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(DatastoreHolder.holder.get(), writer, encoderContext);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public final Class<T> getEncoderClass() {
        return (Class<T>) Expression.class;
    }
}
