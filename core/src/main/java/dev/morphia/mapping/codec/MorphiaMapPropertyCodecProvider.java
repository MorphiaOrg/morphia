package dev.morphia.mapping.codec;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
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

import static dev.morphia.mapping.codec.CodecHelper.document;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
class MorphiaMapPropertyCodecProvider extends MorphiaPropertyCodecProvider {
    private final MorphiaDatastore datastore;
    private final Conversions conversions;

    public MorphiaMapPropertyCodecProvider(MorphiaDatastore datastore, Conversions conversions) {
        this.datastore = datastore;
        this.conversions = conversions;
    }

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
                        return (Codec<T>) registry.get(TypeData.get(Map.class));
                    } catch (CodecConfigurationException e1) {
                        // Ignore and return original exception
                    }
                }
                throw e;
            }
        }
        return null;
    }

    private class MapCodec<K, V> implements Codec<Map<K, V>> {
        private final Class<Map<K, V>> encoderClass;
        private final Class<K> keyType;
        private final Codec<V> codec;
        // creates the maps this codec decodes into; worked out on first use, so a type without a usable constructor
        // still fails when decoding rather than when the codec is built
        private volatile Supplier<Map<K, V>> factory;

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
                    writer.writeName(conversions.convert(key, String.class));
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
                final K key = conversions.convert(reader.readName(), keyType);
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
            Supplier<Map<K, V>> supplier = factory;
            if (supplier == null) {
                supplier = factory();
                factory = supplier;
            }
            return supplier.get();
        }

        private Supplier<Map<K, V>> factory() {
            Class<?> type = encoderClass;
            if (type.isInterface() || type == HashMap.class) {
                return HashMap::new;
            } else if (type == LinkedHashMap.class) {
                return LinkedHashMap::new;
            } else if (type == TreeMap.class) {
                return TreeMap::new;
            }
            try {
                final Constructor<Map<K, V>> constructor = encoderClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return () -> {
                    try {
                        return constructor.newInstance();
                    } catch (Exception e) {
                        throw new CodecConfigurationException(e.getMessage(), e);
                    }
                };
            } catch (Exception e) {
                throw new CodecConfigurationException(e.getMessage(), e);
            }
        }
    }

}
