package dev.morphia.test.mapping.codec;

import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.BsonNull;
import org.testng.annotations.Test;

import static java.util.List.of;
import static org.testng.Assert.*;

public class MorphiaMapCodecTest extends TestBase {

    public MorphiaMapCodecTest() {
        super(buildConfig().packages(of("dev.morphia.test.mapping.codec")));
    }

    @Test
    public void testBasicUsagex() {
        var testClass = new SomeClass(1L, "value");
        getDs().save(testClass);

        var found = getDs().aggregate(SomeClass.class).execute(Map.class).toList();
        assertNotNull(found);
        assertEquals(found.size(), 1);
        var value = found.get(0);
        assertEquals(value.size(), 3);
        assertEquals(value.get("data"), "value");
        assertEquals(value.get("_id"), 1L);
        assertEquals(value.get("_t"), SomeClass.class.getName());
    }

    @Test
    public void testNullCase() {
        var testClass = new SomeClass(1L, BsonNull.VALUE);
        getDs().save(testClass);

        var found = getDs().aggregate(SomeClass.class).execute(Map.class).toList();
        assertNotNull(found);
        assertEquals(found.size(), 1);
        var value = found.get(0);
        assertEquals(value.size(), 3);
        assertNull(value.get("data"));
        assertEquals(value.get("_id"), 1L);
        assertEquals(value.get("_t"), SomeClass.class.getName());
    }

    @Entity(value = "SomeClass")
    static class SomeClass {
        public SomeClass(Long id, Object data) {
            this.id = id;
            this.data = data;
        }

        public @Id Long id;
        public Object data;
    }
}