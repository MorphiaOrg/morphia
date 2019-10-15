package dev.morphia.mapping.lazy;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Reference.class)
public class TestLazyWithMissingReferences extends TestBase {

    @Test(expected = ReferenceException.class)
    public void testMissingRef() {
        final Source source = new Source();
        source.setTarget(new Target());

        getDs().save(source); // does not fail due to pre-initialized Ids

        getDs().find(Source.class).execute().toList();
    }

    @Test(expected = ReferenceException.class)
    public void testMissingRefLazy() {
        final Source e = new Source();
        e.setLazy(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids
        Source source = getDs().find(Source.class)
                               .execute(new FindOptions().limit(1))
                               .tryNext();
        Assert.assertNull(source.getLazy().getFoo());
    }

    @Test(expected = ReferenceException.class)
    public void testMissingRefLazyIgnoreMissing() {
        final Source e = new Source();
        e.setIgnoreMissing(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids

        Source source = getDs().find(Source.class).execute(new FindOptions().limit(1)).tryNext();
        source.getIgnoreMissing().getFoo();
    }

    @Entity
    static class Source {
        @Id
        private ObjectId id = new ObjectId();
        @Reference
        private Target target;
        @Reference(lazy = true)
        private Target lazy;
        @Reference(lazy = true, ignoreMissing = true)
        private Target ignoreMissing;

        public Target getTarget() {
            return target;
        }

        public void setTarget(final Target target) {
            this.target = target;
        }

        public Target getLazy() {
            return lazy;
        }

        public void setLazy(final Target lazy) {
            this.lazy = lazy;
        }

        public Target getIgnoreMissing() {
            return ignoreMissing;
        }

        public void setIgnoreMissing(final Target ignoreMissing) {
            this.ignoreMissing = ignoreMissing;
        }
    }

    @Entity
    public static class Target {
        @Id
        private ObjectId id = new ObjectId();
        private String foo = "bar";

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }
    }

}
