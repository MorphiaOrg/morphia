package dev.morphia.test.mapping.lazy;

import java.util.List;
import java.util.Objects;

import dev.morphia.Datastore;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.test.mapping.ProxyTestBase;
import dev.morphia.test.models.TestEntity;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

@Test(groups = "references")
public class TestLazyIdOnly extends ProxyTestBase {

    @Test
    public void testQueryAfterReferentIsGone() {
        checkForProxyTypes();

        RootEntity root = new RootEntity();
        root.setBar("foo");
        final ReferencedEntity reference = new ReferencedEntity();
        reference.setFoo("bar");

        root.setDontIgnoreMissing(reference);
        root.setIgnoreMissing(reference);

        final Datastore datastore = getDs();
        datastore.save(List.of(reference, root));

        root = datastore.find(RootEntity.class).filter(eq("_id", root.getId())).first();

        ReferencedEntity p = root.dontIgnoreMissing;

        assertIsProxy(p);
        assertNotFetched(p);

        datastore.delete(reference);

        root = datastore.find(RootEntity.class).first();
        assertNotNull(root);

        assertThrows(ReferenceException.class, () -> {
            datastore.find(RootEntity.class).filter(eq("dontIgnoreMissing", p)).first();
        });

        assertNull(datastore.find(RootEntity.class).filter(eq("ignoreMissing", p)).first());

        ReferencedEntity r = root.dontIgnoreMissing;
        assertThrows(ReferenceException.class, () -> {
            r.getFoo();
        });
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
            return Objects.equals(id, that.id) && Objects.equals(foo, that.foo);
        }
    }

    public static class RootEntity extends TestEntity {
        @Reference(idOnly = true, lazy = true, ignoreMissing = false)
        private ReferencedEntity dontIgnoreMissing;
        @Reference(idOnly = true, lazy = true, ignoreMissing = true)
        private ReferencedEntity ignoreMissing;

        private String bar;

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        public ReferencedEntity getDontIgnoreMissing() {
            return dontIgnoreMissing;
        }

        public void setDontIgnoreMissing(ReferencedEntity dontIgnoreMissing) {
            this.dontIgnoreMissing = dontIgnoreMissing;
        }

        public ReferencedEntity getIgnoreMissing() {
            return ignoreMissing;
        }

        public void setIgnoreMissing(ReferencedEntity ignoreMissing) {
            this.ignoreMissing = ignoreMissing;
        }
    }
}