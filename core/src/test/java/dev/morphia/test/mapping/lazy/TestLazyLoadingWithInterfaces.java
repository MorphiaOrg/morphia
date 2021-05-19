package dev.morphia.test.mapping.lazy;

import dev.morphia.mapping.experimental.MorphiaReference;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.mapping.experimental.MorphiaReference.wrap;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Test(groups = "references")
public class TestLazyLoadingWithInterfaces extends ProxyTestBase {
    @SuppressWarnings("unchecked")
    public void interfaces() {
        getMapper().map(InterfaceA.class, InterfaceB.class, ClassA.class, ClassB1.class, ClassB2.class);

        final ClassB1 b1 = new ClassB1("Sandor Clegane");

        final ClassB2 b2 = new ClassB2(42);

        getDs().save(asList(b1, b2));
        final ClassA a = new ClassA();
        a.b = b1;
        a.reference = MorphiaReference.wrap(b2);

        final List<InterfaceB> list = asList(b1, b2);
        final Set<InterfaceB> set = new HashSet<>(list);
        final Map<String, InterfaceB> map = new HashMap<>();
        map.put("key1", b1);
        map.put("key2", b2);

        a.set = wrap(set);
        a.list = wrap(list);
        a.map = wrap(map);

        getDs().save(a);

        final ClassA first = getDs().find(ClassA.class).first();

        assertNotNull(first.b);
        assertEquals(b1.getId(), first.b.getId());

        assertNotNull(first.reference);
        assertEquals(b2, first.reference.get());

        assertNotNull(first.map);
        assertEquals(map, first.map.get());

        assertNotNull(first.list);
        assertEquals(list, first.list.get());

        assertNotNull(first.set);
        assertEquals(set, first.set.get());
    }

}
