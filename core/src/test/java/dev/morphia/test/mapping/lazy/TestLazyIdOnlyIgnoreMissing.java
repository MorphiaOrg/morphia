package dev.morphia.test.mapping.lazy;

import java.util.List;
import java.util.Objects;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;

@Test(groups = "references")
public class TestLazyIdOnlyIgnoreMissing extends ProxyTestBase {
    @Test
    public void testDuplicatesInList() {
        getMapper().map(ListReferences.class, ReferencedEntity.class);
        ListReferences references = new ListReferences();
        ReferencedEntity entity1 = new ReferencedEntity();
        entity1.foo = "1";
        ReferencedEntity entity2 = new ReferencedEntity();
        entity2.foo = "2";
        getDs().save(List.of(entity1, entity2));
        references.list = List.of(entity1, entity2, entity1);
        getDs().save(references);

        ListReferences first = getDs().find(ListReferences.class).first();

        assertEquals(first, references);
    }

    public void testSaveAfterReferentIsGone() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        root.setBar("foo");
        final ReferencedEntity reference = new ReferencedEntity();
        reference.setFoo("bar");

        root.setR(reference);

        final Datastore datastore = getDs();
        final ObjectId id = datastore.save(reference).getId();
        datastore.save(root);

        root = datastore.find(RootEntity.class)
                .filter(eq("_id", root.getId()))
                .first();

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);
        p.getFoo();
        assertFetched(p);

        datastore.delete(reference);

        // Re-Fetch root entity from DB
        root = datastore.find(RootEntity.class)
                .filter(eq("_id", root.getId()))
                .first();

        root.setBar("baz");
        datastore.save(root);
    }

    @Entity
    private static class ListReferences {
        @Id
        private ObjectId id;
        @Reference
        List<ReferencedEntity> list;

        private ListReferences() {
        }

        private ListReferences(List<ReferencedEntity> list) {
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ListReferences)) {
                return false;
            }
            ListReferences that = (ListReferences) o;
            return Objects.equals(list, that.list) && Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, id);
        }
    }

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String string) {
            foo = string;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, foo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReferencedEntity)) {
                return false;
            }
            ReferencedEntity that = (ReferencedEntity) o;
            return Objects.equals(id, that.id)
                    && Objects.equals(foo, that.foo);
        }
    }

    public static class RootEntity extends TestEntity {
        @Reference(idOnly = true, lazy = true, ignoreMissing = true)
        private ReferencedEntity r;

        private String bar;

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        public ReferencedEntity getR() {
            return r;
        }

        public void setR(ReferencedEntity r) {
            this.r = r;
        }
    }
}
