package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


@SuppressWarnings("unchecked")
@Ignore
public class TestReferenceCollection extends ProxyTestBase {
    @Test
    public final void testCreateProxy() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        Origin origin = new Origin();
        final Endpoint endpoint1 = new Endpoint();
        final Endpoint endpoint2 = new Endpoint();

        origin.list.add(endpoint1);
        origin.list.add(endpoint2);

        Collection<Endpoint> lazyEndpoints = origin.lazyList;
        lazyEndpoints.add(endpoint1);
        lazyEndpoints.add(endpoint2);

        getDs().save(endpoint2, endpoint1, origin);

        origin = getDs().get(origin);

        lazyEndpoints = origin.lazyList;
        Assert.assertNotNull(lazyEndpoints);
        assertNotFetched(lazyEndpoints);

        Assert.assertNotNull(lazyEndpoints.iterator().next());
        assertFetched(lazyEndpoints);

        origin = deserialize(origin);

        lazyEndpoints = origin.lazyList;
        Assert.assertNotNull(lazyEndpoints);
        assertNotFetched(lazyEndpoints);

        Assert.assertNotNull(lazyEndpoints.iterator().next());
        assertFetched(lazyEndpoints);

        origin = deserialize(origin);

        getDs().save(origin);
        lazyEndpoints = origin.lazyList;
        assertNotFetched(lazyEndpoints);
    }

    @Test
    public void testOrderingPreserved() throws Exception {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        final Origin origin = new Origin();
        final Endpoint endpoint1 = new Endpoint();
        endpoint1.setFoo("b1");
        origin.lazyList.add(endpoint1);

        final Endpoint endpoint2 = new Endpoint();
        endpoint2.setFoo("b2");
        origin.lazyList.add(endpoint2);
        getDs().save(endpoint1);
        getDs().save(endpoint2);

        Assert.assertEquals("b1", origin.lazyList.iterator().next().foo);

        getDs().save(origin);

        Origin reloaded = getDs().get(origin);
        Assert.assertEquals("b1", reloaded.lazyList.iterator().next().foo);
        Collections.swap(reloaded.lazyList, 0, 1);
        Assert.assertEquals("b2", reloaded.lazyList.iterator().next().foo);

        getDs().save(reloaded);

        reloaded = getDs().get(reloaded);
        final Collection<Endpoint> lbs = reloaded.lazyList;
        Assert.assertEquals(2, lbs.size());
        final Iterator<Endpoint> iterator = lbs.iterator();
        Assert.assertEquals("b2", iterator.next().foo);

    }

    public static class Origin extends TestEntity {
        @Reference
        private final List<Endpoint> list = new ArrayList<Endpoint>();

        @Reference(lazy = true)
        private final List<Endpoint> lazyList = new ArrayList<Endpoint>();

    }

    public static class Endpoint extends TestEntity {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String string) {
            foo = string;
        }

        @Override
        public String toString() {
            return super.toString() + " : id = " + getId() + ", foo=" + foo;
        }
    }

}
