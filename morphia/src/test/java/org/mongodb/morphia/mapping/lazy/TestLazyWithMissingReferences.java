package org.mongodb.morphia.mapping.lazy;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;

public class TestLazyWithMissingReferences extends TestBase {

    @Test(expected = MappingException.class)
    public void testMissingRef() throws Exception {
        final Source source = new Source();
        source.setTarget(new Target());

        getDs().save(source); // does not fail due to pre-initialized Ids

        getDs().find(Source.class).asList();
    }

    @Test(expected = LazyReferenceFetchingException.class)
    public void testMissingRefLazy() throws Exception {
        final Source e = new Source();
        e.setLazy(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids
        Assert.assertNull(getDs().find(Source.class).get().getLazy());
    }

    @Test
    public void testMissingRefLazyIgnoreMissing() throws Exception {
        final Source e = new Source();
        e.setIgnoreMissing(new Target());

        getDs().save(e); // does not fail due to pre-initialized Ids

        try {
            getDs().find(Source.class).get().getIgnoreMissing().foo();
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
