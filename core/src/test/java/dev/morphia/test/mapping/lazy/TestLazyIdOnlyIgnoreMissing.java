package dev.morphia.test.mapping.lazy;

import dev.morphia.Datastore;
import dev.morphia.annotations.Reference;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@Test(groups = "references")
public class TestLazyIdOnlyIgnoreMissing extends ProxyTestBase {
    public void testReferentIsGone() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        root.setBar("foo");
        final ReferencedEntity reference = new ReferencedEntity();
        reference.setFoo("bar");

        root.setR(reference);

        final Datastore datastore = getDs();
        datastore.save(reference);
        datastore.save(root);

        root = datastore.find(RootEntity.class)
                        .filter(eq("_id", root.getId()))
                        .first();

        final ReferencedEntity p = root.r;

        assertIsProxy(p);
        assertNotFetched(p);
        p.getFoo();
        assertFetched(p);

        // delete the referenced entity
        datastore.delete(reference);
        // refresh root entity
        root = datastore.find(RootEntity.class)
                        .filter(eq("_id", root.getId()))
                        .first();

        assertNotNull(root.r);
        assertNotNull(root.getR());
        assertNull(root.r.getFoo());
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

    public static class ReferencedEntity extends TestEntity {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String string) {
            foo = string;
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
