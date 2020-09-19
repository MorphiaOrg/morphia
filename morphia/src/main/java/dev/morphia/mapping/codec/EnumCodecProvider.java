package dev.morphia.mapping.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Defines a CodecProvider for Enum values
 */
public class EnumCodecProvider implements CodecProvider {
    /**
     * Looks up the codec for the type
     *
     * @param type     the type to look up
     * @param registry the registry to use
     * @param <T>      the type of the enum
     * @return the codec if found or null
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
        if (type.isEnum()) {
            return new EnumCodec(type);
        }
        return null;
    }

}
