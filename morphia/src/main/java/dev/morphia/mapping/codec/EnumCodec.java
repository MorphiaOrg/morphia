package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class EnumCodec<T extends Enum<T>> implements Codec<T> {
    private final Class<T> clazz;

    public EnumCodec(final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeString(value.name());
    }

    @Override
    public Class<T> getEncoderClass() {
        return clazz;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return Enum.valueOf(clazz, reader.readString());
    }
}
