package dev.morphia;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.testutil.TestEntity;

import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestGetByKeys extends TestBase {
    @Test
    public final void testGetByKeys() {
        final A a1 = new A();
        final A a2 = new A();

        final Iterable<Key<A>> keys = getDs().save(asList(a1, a2));

        final List<A> reloaded = getDs().getByKeys(keys);

        final Iterator<A> i = reloaded.iterator();
        Assert.assertNotNull(i.next());
        Assert.assertNotNull(i.next());
        Assert.assertFalse(i.hasNext());
    }

    public static class A extends TestEntity {
        private String foo = "bar";
    }

}
