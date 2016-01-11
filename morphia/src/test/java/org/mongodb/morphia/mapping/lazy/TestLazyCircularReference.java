package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import org.mongodb.morphia.testutil.TestEntity;


@Ignore
public class TestLazyCircularReference extends ProxyTestBase {
    @Test
    public final void testCreateProxy() {

        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        reference.parent = root;

        root.r = reference;
        root.r.setFoo("bar");

        getDs().save(reference, root);

        root = getDs().get(root);

        assertNotFetched(root.r);
        Assert.assertEquals("bar", root.r.getFoo());
        assertFetched(root.r);
        Assert.assertEquals("bar", root.r.getFoo());

        // now remove it from DB
        getDs().delete(root.r);

        root = deserialize(root);
        assertNotFetched(root.r);

        try {
            // must fail
            root.r.getFoo();
            Assert.fail("Expected Exception did not happen");
        } catch (LazyReferenceFetchingException expected) {
            // fine
        }

    }

    @Test
    public final void testGetKeyWithoutFetching() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        reference.parent = root;

        root.r = reference;
        reference.setFoo("bar");

        final Key<ReferencedEntity> k = getDs().save(reference);
        final String keyAsString = k.getId().toString();
        getDs().save(root);

        root = getDs().get(root);

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);
        Assert.assertEquals(keyAsString, getDs().getKey(p).getId().toString());
        // still not fetched?
        assertNotFetched(p);
        p.getFoo();
        // should be fetched now.
        assertFetched(p);

    }

    @Test
    public final void testSerialization() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity e1 = new RootEntity();
        final ReferencedEntity e2 = new ReferencedEntity();
        e2.parent = e1;

        e1.r = e2;
        e2.setFoo("bar");

        getDs().save(e2, e1);

        e1 = deserialize(getDs().get(e1));

        assertNotFetched(e1.r);
        Assert.assertEquals("bar", e1.r.getFoo());
        assertFetched(e1.r);

        e1 = deserialize(e1);
        assertNotFetched(e1.r);
        Assert.assertEquals("bar", e1.r.getFoo());
        assertFetched(e1.r);

    }

    @Test
    public final void testShortcutInterface() {
        // TODO us: exclusion does not work properly with maven + junit4
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        reference.parent = root;

        root.r = reference;
        reference.setFoo("bar");

        final Key<ReferencedEntity> k = getDs().save(reference);
        final String keyAsString = k.getId().toString();
        getDs().save(root);

        root = getDs().get(root);

        ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);
        Assert.assertEquals(keyAsString, ((ProxiedEntityReference) p).__getKey().getId().toString());
        // still not fetched?
        assertNotFetched(p);
        p.getFoo();
        // should be fetched now.
        assertFetched(p);

        root = deserialize(root);
        p = root.r;
        assertNotFetched(p);
        p.getFoo();
        // should be fetched now.
        assertFetched(p);

        root = getDs().get(root);
        p = root.r;
        assertNotFetched(p);
        getDs().save(root);
        assertNotFetched(p);
    }

    public static class RootEntity extends TestEntity {
        @Reference(lazy = true)
        private ReferencedEntity r;
        @Reference(lazy = true)
        private ReferencedEntity secondReference;

    }

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        @Reference(lazy = true)
        private RootEntity parent;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String string) {
            foo = string;
        }
    }

}
