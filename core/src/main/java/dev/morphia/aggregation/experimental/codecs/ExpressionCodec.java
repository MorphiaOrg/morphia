package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionCodec<T extends Expression> implements Codec<T> {
    private final Datastore datastore;

    public ExpressionCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(BsonWriter writer, T expression, EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(datastore, writer, encoderContext);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public final Class<T> getEncoderClass() {
        return (Class<T>) Expression.class;
    }

    protected CodecRegistry getCodecRegistry() {
        return datastore.getCodecRegistry();
    }

    protected Datastore getDatastore() {
        return datastore;
    }
}
