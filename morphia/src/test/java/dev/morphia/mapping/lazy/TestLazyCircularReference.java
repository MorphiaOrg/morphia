package dev.morphia.mapping.lazy;


import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.TestEntity;

import static java.util.Arrays.asList;

public class TestLazyCircularReference extends ProxyTestBase {

    @Test
    public void testCircularReferences() {
        RootEntity root = new RootEntity();
        ReferencedEntity first = new ReferencedEntity();
        ReferencedEntity second = new ReferencedEntity();

        getDs().save(asList(root, first, second));
        root.r = first;
        root.secondReference = second;
        first.parent = root;
        second.parent = root;

        getDs().save(asList(root, first, second));

        RootEntity rootEntity = getDs().find(RootEntity.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals(first.getId(), rootEntity.getR().getId());
        Assert.assertEquals(second.getId(), rootEntity.getSecondReference().getId());
        Assert.assertEquals(root.getId(), rootEntity.getR().getParent().getId());
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

    public static class RootEntity extends TestEntity {
        @Reference(lazy = true)
        private ReferencedEntity r;
        @Reference(lazy = true)
        private ReferencedEntity secondReference;

        public ReferencedEntity getR() {
            return r;
        }

        public void setR(final ReferencedEntity r) {
            this.r = r;
        }

        public ReferencedEntity getSecondReference() {
            return secondReference;
        }

        public void setSecondReference(final ReferencedEntity secondReference) {
            this.secondReference = secondReference;
        }
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

        public RootEntity getParent() {
            return parent;
        }

        public void setParent(final RootEntity parent) {
            this.parent = parent;
        }
    }

}
