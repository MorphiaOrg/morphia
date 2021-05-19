package dev.morphia.test.mapping.lazy;


import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;


@SuppressWarnings("unchecked")
public class TestReferenceCollection extends ProxyTestBase {

    @Test
    public void testOrderingPreserved() {
        checkForProxyTypes();

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

        Origin reloaded = getDs().find(Origin.class)
                                 .filter(eq("_id", origin.getId()))
                                 .first();
        Assert.assertEquals("b1", reloaded.lazyList.iterator().next().foo);
        Collections.swap(reloaded.lazyList, 0, 1);
        Assert.assertEquals("b2", reloaded.lazyList.iterator().next().foo);

        getDs().save(reloaded);

        reloaded = getDs().find(Origin.class)
                          .filter(eq("_id", origin.getId()))
                          .first();
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

        public void setFoo(String string) {
            foo = string;
        }

        @Override
        public String toString() {
            return super.toString() + " : id = " + getId() + ", foo=" + foo;
        }
    }

}
