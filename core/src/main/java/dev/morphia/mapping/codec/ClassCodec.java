package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Defines a codec for Class references
 */
public class ClassCodec implements Codec<Class> {
    @Override
    public Class decode(BsonReader reader, DecoderContext decoderContext) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return Class.forName(reader.readString(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void encode(BsonWriter writer, Class value, EncoderContext encoderContext) {
        writer.writeString(value.getName());
    }

    @Override
    public Class<Class> getEncoderClass() {
        return Class.class;
    }
}
