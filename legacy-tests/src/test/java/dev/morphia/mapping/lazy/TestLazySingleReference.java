package dev.morphia.mapping.lazy;


import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.Reference;
import dev.morphia.testutil.TestEntity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static dev.morphia.query.experimental.filters.Filters.eq;


@Category(Reference.class)
public class TestLazySingleReference extends ProxyTestBase {

    @Test
    public final void testCallIdGetterWithoutFetching() {
        Assume.assumeTrue(LazyFeatureDependencies.assertProxyClassesPresent());

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        getDs().save(reference);
        ObjectId id = reference.getId();

        root.r = reference;
        reference.setFoo("bar");
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();

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
    public final void testGetKeyWithoutFetching() {
        Assume.assumeTrue(LazyFeatureDependencies.assertProxyClassesPresent());

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

        root.r = reference;
        reference.setFoo("bar");

        final ObjectId id = getDs().save(reference).getId();
        getDs().save(root);

        RootEntity loaded = getDs().find(RootEntity.class)
                                   .filter(eq("_id", root.getId()))
                                   .first();

        final ReferencedEntity p = loaded.r;

        assertIsProxy(p);
        assertNotFetched(p);
        p.getFoo();
        // should be fetched now.
        assertFetched(p);

    }

    @Test
    @Ignore("entity caching needs to be implemented")
    public final void testSameProxy() {
        Assume.assumeTrue(LazyFeatureDependencies.assertProxyClassesPresent());

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();

        root.r = reference;
        root.secondReference = reference;
        reference.setFoo("bar");

        getDs().save(reference);
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();
        Assert.assertSame(root.r, root.secondReference);
    }

    @Test
    public final void testShortcutInterface() {
        Assume.assumeTrue(LazyFeatureDependencies.assertProxyClassesPresent());

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        final ReferencedEntity second = new ReferencedEntity();

        root.r = reference;
        root.secondReference = second;
        reference.setFoo("bar");

        getDs().save(reference).getId();
        getDs().save(second);
        getDs().save(root);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();

        ReferencedEntity referenced = root.r;

        assertIsProxy(referenced);
        assertNotFetched(referenced);
        assertNotFetched(root.secondReference);
        referenced.getFoo();
        // should be fetched now.
        assertFetched(referenced);
        assertNotFetched(root.secondReference);
        root.secondReference.getFoo();
        assertFetched(root.secondReference);

        root = getDs().find(RootEntity.class)
                      .filter(eq("_id", root.getId()))
                      .first();
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

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReferencedEntity)) {
                return false;
            }

            final ReferencedEntity that = (ReferencedEntity) o;

            return getFoo() != null ? getFoo().equals(that.getFoo()) : that.getFoo() == null;
        }

        @Override
        public int hashCode() {
            return getFoo() != null ? getFoo().hashCode() : 0;
        }
    }

}
