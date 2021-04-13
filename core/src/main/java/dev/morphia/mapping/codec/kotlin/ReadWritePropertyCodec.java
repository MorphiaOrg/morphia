package dev.morphia.mapping.codec.kotlin;

import kotlin.properties.ReadWriteProperty;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.2
 */
@SuppressWarnings("unchecked")
public class ReadWritePropertyCodec implements Codec<Object> {
    private final Codec<Object> codec;

    public ReadWritePropertyCodec(Codec<?> codec) {
        this.codec = (Codec<Object>) codec;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext context) {
        return codec.decode(reader, context);
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext context) {
        context.encodeWithChildContext(codec, writer, value);
    }

    @Override
    public Class<Object> getEncoderClass() {
        return (Class<Object>) ((Class) ReadWriteProperty.class);
    }
}
