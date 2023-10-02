package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;

public abstract class BaseExpressionCodec<T extends Expression> implements Codec<T> {
    protected MorphiaDatastore datastore;

    public BaseExpressionCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }
}
