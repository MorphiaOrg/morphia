package dev.morphia.mapping.lazy;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import dev.morphia.query.FindOptions;

public class TestLazyWithMissingReferences extends TestBase {

    @Test(expected = MappingException.class)
    public void testMissingRef() {
        final Source source = new Source();
        source.setTarget(new Target());

        getDs().save(source); // does not fail due to pre-initialized Ids

        toList(getDs().find(Source.class).find());
    }

    @Test(expected = LazyReferenceFetchingException.class)
    public void testMissingRefLazy() {
        final Source e = new Source();
        e.setLazy(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids
        Assert.assertNull(getDs().find(Source.class).find(new FindOptions().limit(1)).tryNext().getLazy());
    }

    @Test
    public void testMissingRefLazyIgnoreMissing() {
        final Source e = new Source();
        e.setIgnoreMissing(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids

        try {
            getDs().find(Source.class).find(new FindOptions().limit(1)).tryNext().getIgnoreMissing().foo();
        } catch (RuntimeException re) {
            Assert.assertEquals("Cannot dispatch method foo", re.getMessage());
        }
    }

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

    static class Target {
        @Id
        private ObjectId id = new ObjectId();
        private String foo = "bar";

        void foo() {
        }

    }

}
