package dev.morphia.internal;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Encodes each document with the codec for its own class, so one {@code insertMany} can carry entities of several classes
 * stored in the same collection. Encoding only.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class DispatchingCodec implements CollectibleCodec<Object> {
    private final CodecRegistry registry;

    /**
     * @param registry the registry to find each document's codec in
     */
    public DispatchingCodec(CodecRegistry registry) {
        this.registry = registry;
    }

    private Codec codec(Object value) {
        return registry.get(value.getClass());
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        codec(value).encode(writer, value, encoderContext);
    }

    @Override
    public Object generateIdIfAbsentFromDocument(Object document) {
        return codec(document) instanceof CollectibleCodec collectible
                ? collectible.generateIdIfAbsentFromDocument(document)
                : document;
    }

    @Override
    public boolean documentHasId(Object document) {
        return codec(document) instanceof CollectibleCodec collectible && collectible.documentHasId(document);
    }

    @Override
    public BsonValue getDocumentId(Object document) {
        if (codec(document) instanceof CollectibleCodec collectible) {
            return collectible.getDocumentId(document);
        }
        throw new IllegalStateException("The document does not contain an _id");
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }
}
