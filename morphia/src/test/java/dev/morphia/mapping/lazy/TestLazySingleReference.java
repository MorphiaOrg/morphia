package dev.morphia.mapping.lazy;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.testutil.TestEntity;


public class TestLazySingleReference extends ProxyTestBase {

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
