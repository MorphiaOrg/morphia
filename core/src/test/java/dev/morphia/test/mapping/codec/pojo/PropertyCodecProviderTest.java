package dev.morphia.test.mapping.codec.pojo;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.MyPropertyCodecProvider;
import dev.morphia.test.TestBase;
import dev.morphia.test.mapping.codec.pojo.PropertyCodecProviderTest.MyEntity.TheEnum;

public class PropertyCodecProviderTest extends TestBase {
    public PropertyCodecProviderTest() {
        super(MapperOptions.builder()
                .propertyCodecProvider(new MyPropertyCodecProvider())
                .build());
    }

    @Test
    public void testEnumSetAndEnumMapProperty() {
        MyEntity entity = new MyEntity();
        entity.enumSet = EnumSet.of(TheEnum.ONE);
        

        getDs().save(entity);

        try {
			entity = getDs().find(MyEntity.class).first();
			fail("should throw exception");
		} catch (RuntimeException e) {
			assertEquals(e.getMessage(), "EnumSet codec registered and found");
		}
        
        entity.enumMap = new EnumMap<>(TheEnum.class);
        entity.enumMap.put(TheEnum.TWO, "TWO");
        entity.enumSet = null;
        
        getDs().save(entity);

        try {
			entity = getDs().find(MyEntity.class).first();
			fail("should throw exception");
		} catch (RuntimeException e) {
			assertEquals(e.getMessage(), "EnumMap codec registered and found");
		}
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
}
