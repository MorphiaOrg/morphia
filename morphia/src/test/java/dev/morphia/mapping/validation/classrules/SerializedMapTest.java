package dev.morphia.mapping.validation.classrules;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Serialized;
import dev.morphia.testutil.TestEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class SerializedMapTest extends TestBase {

    @Test
    public void testSerialization() throws Exception {
        Map1 map1 = new Map1();
        map1.shouldBeOk.put(3, new Foo("peter"));
        map1.shouldBeOk.put(27, new Foo("paul"));

        getDs().save(map1);
        map1 = getDs().get(map1);

        Assert.assertEquals("peter", map1.shouldBeOk.get(3).id);
        Assert.assertEquals("paul", map1.shouldBeOk.get(27).id);

    }

    @Test
    public void testSerialization2() throws Exception {
        Map2 map2 = new Map2();
        map2.shouldBeOk.put(3, new Foo("peter"));
        map2.shouldBeOk.put(27, new Foo("paul"));

        getDs().save(map2);
        map2 = getDs().get(map2);

        Assert.assertEquals("peter", map2.shouldBeOk.get(3).id);
        Assert.assertEquals("paul", map2.shouldBeOk.get(27).id);

    }

    public static class Map1 extends TestEntity {
        @Serialized(disableCompression = false)
        private final Map<Integer, Foo> shouldBeOk = new HashMap();

    }

    public static class Map2 extends TestEntity {
        @Serialized(disableCompression = true)
        private final Map<Integer, Foo> shouldBeOk = new HashMap();

    }

    public static class Foo implements Serializable {

        private final String id;

        public Foo(final String id) {
            this.id = id;
        }
    }
}
