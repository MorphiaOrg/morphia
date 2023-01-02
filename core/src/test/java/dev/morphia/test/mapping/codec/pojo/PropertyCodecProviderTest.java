package dev.morphia.test.mapping.codec.pojo;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.CollectionCodec;
import dev.morphia.mapping.codec.EnumCodec;
import dev.morphia.mapping.codec.MorphiaMapPropertyCodecProvider.MapCodec;
import dev.morphia.mapping.codec.MorphiaPropertyCodecProvider;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.codec.pojo.PropertyCodecProviderTest.MyEntity.TheEnum;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PropertyCodecProviderTest extends TestBase {
    public PropertyCodecProviderTest() {
        super(MapperOptions.builder()
                .propertyCodecProvider(new MyCodecProvider())
                .build());
    }

    @Test
    public void testEnumSetPRoperty() {
        MyEntity entity = new MyEntity();
        entity.enumSet = EnumSet.of(TheEnum.ONE);
        entity.enumMap = new EnumMap<>(TheEnum.class);
        entity.enumMap.put(TheEnum.TWO, "TWO");
        entity.stringList = List.of("item1");
        entity.stringMap = Map.of("key", "value");

        getDs().save(entity);

        entity = getDs().find(MyEntity.class).first();

        assertEquals(TheEnum.ONE, entity.enumSet.iterator().next());
        assertEquals("TWO", entity.enumMap.get(TheEnum.TWO));
        
        assertEquals("item1", entity.stringList.iterator().next());
        assertEquals("value", entity.stringMap.get("key"));
    }

    @Entity
    public static class MyEntity {
        @Id
        public ObjectId id;

        public EnumSet<TheEnum> enumSet;
        public EnumMap<TheEnum, String> enumMap;
        
        public List<String> stringList;
        public Map<String, String> stringMap;

        public enum TheEnum {
            ONE,
            TWO
        }
    }

    public static class MyCodecProvider extends MorphiaPropertyCodecProvider {

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

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected Collection<T> getInstance() {
                return EnumSet.noneOf(((EnumCodec) getCodec()).getEncoderClass());
            }

        }

        public static class EnumMapCodec<K, V> extends MapCodec<K, V> {

            EnumMapCodec(Class<Map<K, V>> encoderClass, Class<K> keyType, Codec<V> codec) {
                super(encoderClass, keyType, codec);
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected Map<K, V> getInstance() {
                return new EnumMap(keyType);
            }

        }
    }
}
