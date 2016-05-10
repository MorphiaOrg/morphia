package org.mongodb.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.IdGetter;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import org.mongodb.morphia.testutil.TestEntity;


public class TestLazySingleReference extends ProxyTestBase {
    @Ignore
    @Test
    public final void testCreateProxy() {

        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity referenced = new ReferencedEntity();

        root.r = referenced;
        root.r.setFoo("bar");

        getDs().save(referenced);
        getDs().save(root);

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
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

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
    public final void testCallIdGetterWithoutFetching() {
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        ObjectId id = reference.getId();
        getDs().save(reference);

        root.r = reference;
        reference.setFoo("bar");
        getDs().save(root);

        root = getDs().get(root);

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);

        ObjectId idFromProxy = p.getId();
        Assert.assertEquals(id, idFromProxy);

        // Since getId() is annotated with @IdGetter, it should not cause the
        // referenced entity to be fetched
        assertNotFetched(p);

        p.getFoo();

        // Calling getFoo() should have caused the referenced entity to be fetched
        assertFetched(p);

    }

    @Test
    public final void testSameProxy() {
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

        root.r = reference;
        root.secondReference = reference;
        reference.setFoo("bar");

        getDs().save(reference);
        getDs().save(root);

        root = getDs().get(root);
        Assert.assertSame(root.r, root.secondReference);
    }

    @Ignore
    @Test
    public final void testSerialization() {
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity e1 = new RootEntity();
        final ReferencedEntity e2 = new ReferencedEntity();

        e1.r = e2;
        e2.setFoo("bar");

        getDs().save(e2);
        getDs().save(e1);

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
        if (!LazyFeatureDependencies.testDependencyFullFilled()) {
            return;
        }

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        final ReferencedEntity second = new ReferencedEntity();

        root.r = reference;
        root.secondReference = second;
        reference.setFoo("bar");

        final Key<ReferencedEntity> k = getDs().save(reference);
        getDs().save(second);
        final Object key = k.getId();
        getDs().save(root);

        root = getDs().get(root);

        ReferencedEntity referenced = root.r;

        assertIsProxy(referenced);
        assertNotFetched(referenced);
        Assert.assertEquals(key, ((ProxiedEntityReference) referenced).__getKey().getId());
        // still not fetched?
        assertNotFetched(referenced);
        assertNotFetched(root.secondReference);
        referenced.getFoo();
        // should be fetched now.
        assertFetched(referenced);
        assertNotFetched(root.secondReference);
        root.secondReference.getFoo();
        assertFetched(root.secondReference);

        root = getDs().get(root);
        assertNotFetched(root.r);
        assertNotFetched(root.secondReference);
        getDs().save(root);
        assertNotFetched(root.r);
        assertNotFetched(root.secondReference);
    }

    public static class RootEntity extends TestEntity {
        @Reference(lazy = true)
        private ReferencedEntity r;
        @Reference(lazy = true)
        private ReferencedEntity secondReference;

    }

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        @Override
        @IdGetter
        public ObjectId getId() {
            return super.getId();
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String string) {
            foo = string;
        }
    }

}
