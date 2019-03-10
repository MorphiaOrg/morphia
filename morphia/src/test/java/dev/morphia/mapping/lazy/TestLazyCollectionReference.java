package dev.morphia.mapping.lazy;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import dev.morphia.testutil.TestEntity;


public class TestLazyCollectionReference extends ProxyTestBase {
    @Test(expected = LazyReferenceFetchingException.class)
    public final void testCreateProxy() {

        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        // Create a root entity with 2 referenced entities
        RootEntity root = new RootEntity();
        final ReferencedEntity referenced1 = new ReferencedEntity();
        referenced1.setFoo("bar1");
        final ReferencedEntity referenced2 = new ReferencedEntity();
        referenced2.setFoo("bar2");

        List<ReferencedEntity> references = new ArrayList<ReferencedEntity>();
        references.add(referenced1);
        references.add(referenced2);
        root.references = references;

        // save to DB
        getDs().save(referenced1);
        getDs().save(referenced2);
        getDs().save(root);

        // read root entity from DB
        root = getDs().get(root);
        assertNotFetched(root.references);

        // use the lazy collection
        Collection<ReferencedEntity> retrievedReferences = root.references;
        Assert.assertEquals(2, retrievedReferences.size());
        assertFetched(root.references);
        Iterator<ReferencedEntity> it = retrievedReferences.iterator();
        Assert.assertEquals("bar1", it.next().getFoo());
        Assert.assertEquals("bar2", it.next().getFoo());

        // read root entity from DB again
        root = getDs().get(root);
        assertNotFetched(root.references);

        // remove the first referenced entity from DB
        getDs().delete(referenced1);

        // must fail
        root.references.size();

    }

    public static class RootEntity extends TestEntity {
        @Reference(lazy = true)
        private Collection<ReferencedEntity> references;
    }

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        public void setFoo(final String string) {
            foo = string;
        }

        public String getFoo() {
            return foo;
        }
    }

}
