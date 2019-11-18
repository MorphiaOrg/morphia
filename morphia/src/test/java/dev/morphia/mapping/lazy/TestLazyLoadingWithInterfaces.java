package dev.morphia.mapping.lazy;

import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.mapping.codec.references.ReferenceProxy;
import dev.morphia.mapping.experimental.MorphiaReference;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.mapping.experimental.MorphiaReference.wrap;
import static java.util.Arrays.asList;

@Category(Reference.class)
public class TestLazyLoadingWithInterfaces extends ProxyTestBase {
    @SuppressWarnings("unchecked")
    @Test
    public void interfaces() {
        getMapper().map(InterfaceA.class, InterfaceB.class, ClassA.class, B1.class, B2.class);

        final B1 b1 = new B1("Sandor Clegane");

        final B2 b2 = new B2(42);

        getDs().save(asList(b1, b2));
        final ClassA a = new ClassA();
        a.b = b1;
        a.reference = MorphiaReference.<InterfaceB>wrap(b2);

        final List<InterfaceB> list = asList(b1, b2);
        final Set<InterfaceB> set = new HashSet<InterfaceB>(list);
        final Map<String, InterfaceB> map = new HashMap<String, InterfaceB>();
        map.put("key1", b1);
        map.put("key2", b2);

        a.set = wrap(set);
        a.list = wrap(list);
        a.map = wrap(map);

        getDs().save(a);

        final ClassA first = getDs().find(ClassA.class).first();

        Assert.assertNotNull(first.b);
        Assert.assertEquals(b1.getId(), first.b.getId());

        Assert.assertNotNull(first.reference);
        Assert.assertEquals(b2, first.reference.get());

        Assert.assertNotNull(first.map);
        Assert.assertEquals(map, first.map.get());

        Assert.assertNotNull(first.list);
        Assert.assertEquals(list.size(), first.list.get().size());

        Assert.assertNotNull(first.set);
        Assert.assertEquals(set.size(), first.set.get().size());
    }

}
