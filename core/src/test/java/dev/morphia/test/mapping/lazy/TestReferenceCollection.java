package dev.morphia.test.mapping.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

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

        Assertions.assertEquals(origin.lazyList.iterator().next().foo, "b1");

        getDs().save(origin);

        Origin reloaded = getDs().find(Origin.class)
                .filter(eq("_id", origin.getId()))
                .first();
        Assertions.assertEquals(reloaded.lazyList.iterator().next().foo, "b1");
        Collections.swap(reloaded.lazyList, 0, 1);
        Assertions.assertEquals(reloaded.lazyList.iterator().next().foo, "b2");

        getDs().save(reloaded);

        reloaded = getDs().find(Origin.class)
                .filter(eq("_id", origin.getId()))
                .first();
        final Collection<Endpoint> lbs = reloaded.lazyList;
        Assertions.assertEquals(lbs.size(), 2);
        final Iterator<Endpoint> iterator = lbs.iterator();
        Assertions.assertEquals(iterator.next().foo, "b2");

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
