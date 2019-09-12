package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.experimental.ListReferenceCodec;
import dev.morphia.mapping.experimental.MapReferenceCodec;
import dev.morphia.mapping.experimental.MorphiaReferenceCodec;
import dev.morphia.mapping.experimental.SetReferenceCodec;
import dev.morphia.mapping.experimental.SingleReferenceCodec;
import dev.morphia.query.CriteriaContainerCodec;
import dev.morphia.query.FieldCriteriaCodec;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.MapCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MorphiaTypesCodecProvider implements CodecProvider {
    private Mapper mapper;
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<Class<?>, Codec<?>>();

    public MorphiaTypesCodecProvider(final Mapper mapper) {
        this.mapper = mapper;
        BsonTypeClassMap typeMap = new BsonTypeClassMap();

        addCodec(new ClassCodec());
        addCodec(new CriteriaContainerCodec(mapper));
        addCodec(new FieldCriteriaCodec(mapper));
        addCodec(new CenterCodec());
        addCodec(new HashMapCodec());
        addCodec(new KeyCodec(mapper));
        addCodec(new LocaleCodec());
        addCodec(new ObjectCodec(mapper));
        addCodec(new ShapeCodec());
        addCodec(new QueryCodec(mapper));
        addCodec(new URICodec());
        addCodec(new MorphiaReferenceCodec(mapper, typeMap));
        addCodec(new SingleReferenceCodec(mapper, typeMap));
        addCodec(new ListReferenceCodec(mapper, typeMap));
        addCodec(new MapReferenceCodec(mapper, typeMap));
        addCodec(new SetReferenceCodec(mapper, typeMap));

        List.of(boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            long.class, Long.class,
            short.class, Short.class).forEach(c -> {
            addCodec(new TypedArrayCodec(c, mapper));
        });
    }

    protected <T> void addCodec(final Codec<T> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        final Codec<T> codec = (Codec<T>) codecs.get(clazz);
        if (codec != null) {
            return codec;
        } else if (clazz.isArray() && !clazz.getComponentType().equals(byte.class)) {
            return (Codec<T>) new ArrayCodec(mapper, clazz);
        } else {
            return null;
        }
    }

    private static class HashMapCodec extends MapCodec {
        @Override
        public Class<Map<String, Object>> getEncoderClass() {
            return (Class<Map<String, Object>>) ((Class) HashMap.class);
        }
    }

}
