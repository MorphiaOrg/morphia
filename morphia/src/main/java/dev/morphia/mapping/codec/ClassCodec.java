package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ClassCodec implements Codec<Class> {
    @Override
    public Class decode(final BsonReader reader, final DecoderContext decoderContext) {
        try {
            return Class.forName(reader.readString());
        } catch (ClassNotFoundException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void encode(final BsonWriter writer, final Class value, final EncoderContext encoderContext) {
        writer.writeString(value.getName());
    }

    @Override
    public Class<Class> getEncoderClass() {
        return Class.class;
    }
}
