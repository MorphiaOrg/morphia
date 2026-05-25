package dev.morphia.test.mapping.lazy;

import dev.morphia.Datastore;
import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static java.util.Arrays.asList;

@Tag("references")
@Disabled("references need caching")
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

        RootEntity rootEntity = getDs().find(RootEntity.class).iterator().tryNext();
        Assertions.assertEquals(rootEntity.getR().getId(), first.getId());
        Assertions.assertEquals(rootEntity.getSecondReference().getId(), second.getId());
        Assertions.assertEquals(rootEntity.getR().getParent().getId(), root.getId());
    }

    @Test
    public void testGetKeyWithoutFetching() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        final ReferencedEntity reference = new ReferencedEntity();
        reference.parent = root;

        root.r = reference;
        reference.setFoo("bar");

        final ObjectId id = getDs().save(reference).getId();
        getDs().save(root);

        final Datastore datastore = getDs();
        root = datastore.find(RootEntity.class)
                .filter(eq("_id", root.getId()))
                .first();

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);
        p.getFoo();
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

        public void setR(ReferencedEntity r) {
            this.r = r;
        }

        public ReferencedEntity getSecondReference() {
            return secondReference;
        }

        public void setSecondReference(ReferencedEntity secondReference) {
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

        public void setFoo(String string) {
            foo = string;
        }

        public RootEntity getParent() {
            return parent;
        }

        public void setParent(RootEntity parent) {
            this.parent = parent;
        }
    }
}
