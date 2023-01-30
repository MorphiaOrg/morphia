package dev.morphia.mapping.codec;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import dev.morphia.mapping.codec.MorphiaMapPropertyCodecProvider.MapCodec;

public class MyPropertyCodecProvider extends MorphiaPropertyCodecProvider {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (type.getType().equals(EnumSet.class)) {
            return new EnumSetCodec(type.getType(), registry.get(type.getTypeParameters().get(0)));
        } else if (type.getType().equals(EnumMap.class)) {
            return new EnumMapCodec(type.getType(), type.getTypeParameters().get(0).getType(),
                    registry.get(type.getTypeParameters().get(1)));
        }
        return null;
    }

    public static class EnumSetCodec<T> extends CollectionCodec<T> {

        protected EnumSetCodec(Class<Collection<T>> encoderClass, Codec<T> codec) {
            super(encoderClass, codec);
        }

        @Override
        protected Collection<T> getInstance() {
        	throw new RuntimeException("EnumSet codec registered and found");
        }

    }

    public static class EnumMapCodec<K, V> extends MapCodec<K, V> {

        EnumMapCodec(Class<Map<K, V>> encoderClass, Class<K> keyType, Codec<V> codec) {
            super(encoderClass, keyType, codec);
        }

        @Override
        protected Map<K, V> getInstance() {
        	throw new RuntimeException("EnumMap codec registered and found");
        }

    }
}
