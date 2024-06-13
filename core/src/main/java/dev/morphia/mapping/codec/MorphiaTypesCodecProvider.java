package dev.morphia.mapping.codec;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.expressions.FieldsCodec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Defines a provider of codecs for Morphia's types
 */
@SuppressWarnings("unchecked")
public class MorphiaTypesCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();
    private MorphiaDatastore datastore;

    /**
     * Create the provider
     *
     */
    public MorphiaTypesCodecProvider(MorphiaDatastore datastore) {
        this.datastore = datastore;
        addCodec(new MorphiaDateCodec(datastore));
        addCodec(new MorphiaMapCodec(datastore));
        addCodec(new MorphiaLocalDateTimeCodec(datastore));
        addCodec(new MorphiaLocalTimeCodec());
        addCodec(new PolygonCoordinatesCodec());
        addCodec(new ClassCodec());
        addCodec(new LocaleCodec());
        addCodec(new ObjectCodec(datastore));
        addCodec(new URICodec());
        addCodec(new ByteWrapperArrayCodec());
        addCodec(new BitSetCodec());
        addCodec(new FieldsCodec(datastore));

        List.of(boolean.class, Boolean.class,
                char.class, Character.class,
                double.class, Double.class,
                float.class, Float.class,
                int.class, Integer.class,
                long.class, Long.class,
                short.class, Short.class).forEach(c -> addCodec(new TypedArrayCodec(datastore, c)));
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
            return (Codec<T>) new ArrayCodec(datastore, clazz);
        } else {
            return null;
        }
    }
}
