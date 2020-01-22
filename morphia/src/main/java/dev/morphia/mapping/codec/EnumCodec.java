package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * An enum codec
 *
 * @param <T> the type of the enum
 * @morphia.internal
 * @since 2.0
 */
public class EnumCodec<T extends Enum<T>> implements Codec<T> {
    private final Class<T> type;

    /**
     * Creates a codec for the given type
     *
     * @param type the type
     */
    public EnumCodec(final Class<T> type) {
        this.type = type;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeString(value.name());
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return Enum.valueOf(type, reader.readString());
    }

    @Override
    public Class<T> getEncoderClass() {
        return type;
    }
}
