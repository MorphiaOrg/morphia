package dev.morphia.mapping.codec.filters;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.filters.Filter;
import dev.morphia.sofia.Sofia;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public abstract class BaseFilterCodec<T extends Filter> implements Codec<T> {
    protected MorphiaDatastore datastore;

    public BaseFilterCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void writeNamedValue(@Nullable String name, @Nullable Object value, MorphiaDatastore datastore, BsonWriter writer,
            EncoderContext encoderContext) {
        writer.writeName(name);
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void writeUnnamedValue(@Nullable Object value, MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }

}
