package dev.morphia.mapping.lazy;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import dev.morphia.Datastore;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import dev.morphia.testutil.TestEntity;
import org.junit.experimental.categories.Category;


@Category(Reference.class)
public class TestLazyCollectionReference extends ProxyTestBase {
    @Test(expected = LazyReferenceFetchingException.class)
    public final void testCreateProxy() {

        Assume.assumeTrue(LazyFeatureDependencies.assertProxyClassesPresent());

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
        Assert.assertEquals(2, root.references.size());

        // use the lazy collection
        Collection<ReferencedEntity> retrievedReferences = root.references;
        Assert.assertEquals(2, retrievedReferences.size());
        Iterator<ReferencedEntity> it = retrievedReferences.iterator();
        Assert.assertEquals("bar1", it.next().getFoo());
        Assert.assertEquals("bar2", it.next().getFoo());

        final Datastore datastore = getDs();
        root = datastore.find(RootEntity.class)
                        .filter("_id", root.getId())
                        .first();

        getDs().delete(referenced1);

        root.references.iterator();
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
