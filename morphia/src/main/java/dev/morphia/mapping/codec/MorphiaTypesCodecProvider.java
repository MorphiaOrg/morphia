package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.query.CriteriaContainerCodec;
import dev.morphia.query.FieldCriteriaCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.MapCodec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MorphiaTypesCodecProvider extends ValueCodecProvider {
    private final Codec<?> arrayCodec;

    public MorphiaTypesCodecProvider(final Mapper mapper) {
        addCodec(new KeyCodec(mapper));
        addCodec(new ClassCodec());
        addCodec(new BooleanArrayCodec(mapper));
        addCodec(new ShortArrayCodec(mapper));
        addCodec(new IntArrayCodec(mapper));
        addCodec(new LongArrayCodec(mapper));
        addCodec(new FloatArrayCodec(mapper));
        addCodec(new DoubleArrayCodec(mapper));
        addCodec(new StringArrayCodec(mapper));
        addCodec(new HashMapCodec());
        addCodec(new URICodec());
        addCodec(new CenterCodec());
        addCodec(new ShapeCodec());
        addCodec(new ObjectCodec(mapper));
        addCodec(new QueryCodec(mapper));
        addCodec(new FieldCriteriaCodec(mapper));
        addCodec(new CriteriaContainerCodec(mapper));
        arrayCodec = new ArrayCodec(mapper);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        final Codec<T> codec = super.get(clazz, registry);
        if (codec != null) {
            return codec;
        } else if (clazz.isArray() && !clazz.getComponentType().equals(byte.class)) {
            return (Codec<T>) arrayCodec;
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
