package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a provider of codecs for Morphia's types
 */
@SuppressWarnings("unchecked")
public class MorphiaTypesCodecProvider implements CodecProvider {
    private final Mapper mapper;
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();

    /**
     * Create the provider
     *
     * @param mapper the mapper to use
     */
    public MorphiaTypesCodecProvider(Mapper mapper) {
        this.mapper = mapper;

        addCodec(new MorphiaDateCodec(mapper));
        addCodec(new MorphiaMapCodec(mapper));
        addCodec(new MorphiaLocalDateTimeCodec(mapper));
        addCodec(new MorphiaLocalTimeCodec());
        addCodec(new ClassCodec());
        addCodec(new CenterCodec());
        addCodec(new KeyCodec(mapper));
        addCodec(new LocaleCodec());
        addCodec(new ObjectCodec(mapper));
        addCodec(new ShapeCodec());
        addCodec(new LegacyQueryCodec(mapper));
        addCodec(new MorphiaQueryCodec(mapper));
        addCodec(new URICodec());
        addCodec(new ByteWrapperArrayCodec());

        List.of(boolean.class, Boolean.class,
            char.class, Character.class,
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            long.class, Long.class,
            short.class, Short.class).forEach(c -> addCodec(new TypedArrayCodec(c, mapper)));

    }

    protected <T> void addCodec(Codec<T> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        Codec<T> codec = (Codec<T>) codecs.get(clazz);
        if (codec != null) {
            return codec;
        } else if (AbstractMap.class.isAssignableFrom(clazz)) {
            return (Codec<T>) get(Map.class, registry);
        } else if (clazz.isArray() && !clazz.getComponentType().equals(byte.class)) {
            return (Codec<T>) new ArrayCodec(mapper, clazz);
        } else {
            return null;
        }
    }
}
