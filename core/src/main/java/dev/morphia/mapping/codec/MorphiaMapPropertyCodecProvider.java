package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

@SuppressWarnings("unchecked")
class MorphiaMapPropertyCodecProvider extends MorphiaPropertyCodecProvider {
    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (Map.class.isAssignableFrom(type.getType())) {
            final List<? extends TypeWithTypeParameters<?>> typeParameters = type.getTypeParameters();
            TypeWithTypeParameters<?> keyType = getType(typeParameters, 0);
            final TypeWithTypeParameters<?> valueType = getType(typeParameters, 1);

            try {
                return new MapCodec(type.getType(), keyType.getType(), registry.get(valueType));
            } catch (CodecConfigurationException e) {
                if (valueType.getType().equals(Object.class)) {
                    try {
                        return (Codec<T>) registry.get(TypeData.builder(Map.class).build());
                    } catch (CodecConfigurationException e1) {
                        // Ignore and return original exception
                    }
                }
                throw e;
            }
        } else if (Enum.class.isAssignableFrom(type.getType())) {
            return new EnumCodec(type.getType());
        }
        return null;
    }

    private static class MapCodec<K, V> implements Codec<Map<K, V>> {
        private final Class<Map<K, V>> encoderClass;
        private final Class<K> keyType;
        private final Codec<V> codec;

        MapCodec(Class<Map<K, V>> encoderClass, Class<K> keyType, Codec<V> codec) {
            this.encoderClass = encoderClass;
            this.keyType = keyType;
            this.codec = codec;
        }

        @Override
        public void encode(BsonWriter writer, Map<K, V> map, EncoderContext encoderContext) {
            document(writer, () -> {
                for (Entry<K, V> entry : map.entrySet()) {
                    final K key = entry.getKey();
                    writer.writeName(Conversions.convert(key, String.class));
                    if (entry.getValue() == null) {
                        writer.writeNull();
                    } else {
                        codec.encode(writer, entry.getValue(), encoderContext);
                    }
                }
            });
        }

        @Override
        public Map<K, V> decode(BsonReader reader, DecoderContext context) {
            reader.readStartDocument();
            Map<K, V> map = getInstance();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                final K key = Conversions.convert(reader.readName(), keyType);
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    map.put(key, null);
                    reader.readNull();
                } else {
                    map.put(key, codec.decode(reader, context));
                }
            }
            reader.readEndDocument();
            return map;
        }

        @Override
        public Class<Map<K, V>> getEncoderClass() {
            return encoderClass;
        }

        private Map<K, V> getInstance() {
            if (encoderClass.isInterface()) {
                return new HashMap<>();
            }
            try {
                final Constructor<Map<K, V>> constructor = encoderClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new CodecConfigurationException(e.getMessage(), e);
            }
        }
    }

}
