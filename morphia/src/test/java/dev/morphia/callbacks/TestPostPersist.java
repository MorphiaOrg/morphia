package dev.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostPersist;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;


public class TestPostPersist extends TestBase {

    @Test
    public void testBulkLifecycleEvents() {
        TestObject to1 = new TestObject("post value 1");
        TestObject to2 = new TestObject("post value 2");
        getAds().insert(asList(to1, to2));

        assertNotNull(to1.id);
        assertNotNull(to1.one);
        assertNotNull(to2.id);
        assertNotNull(to2.one);
    }

    @Test
    public void testCallback() throws Exception {
        final ProblematicPostPersistEntity p = new ProblematicPostPersistEntity();
        getDs().save(p);
        Assert.assertTrue(p.called);
        Assert.assertTrue(p.i.innerCalled);
    }

    public static class ProblematicPostPersistEntity {
        private final Inner i = new Inner();
        @Id
        private ObjectId id;
        private boolean called;

        static class Inner {
            private boolean innerCalled;

            @PostPersist
            void m2() {
                innerCalled = true;
            }
        }

        @PostPersist
        void m1() {
            called = true;
        }
    }

    @Entity
    public static class TestObject {

        private final String value;
        @Id
        private ObjectId id;
        private String one;

        public TestObject(final String value) {

            this.value = value;
        }

        @PostPersist
        public void doIt() {
            if (one != null) {
                throw new RuntimeException("@PostPersist methods should only be called once");
            }
            one = value;
        }
    }
}
