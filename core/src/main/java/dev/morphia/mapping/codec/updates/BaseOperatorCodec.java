package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.sofia.Sofia;

import org.bson.BsonReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;

public abstract class BaseOperatorCodec<T extends UpdateOperator> implements Codec<T> {
    protected MorphiaDatastore datastore;

    public BaseOperatorCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

}
