package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostPersist;


public class TestProblematicPostPersistEntity extends TestBase {

    public static class ProblematicPostPersistEntity {
        @Id
        private ObjectId id;

        private final Inner i = new Inner();

        private boolean called;

        @PostPersist
        void m1() {
            called = true;
        }

        static class Inner {
            private boolean called;

            private String foo = "foo";

            @PostPersist
            void m2() {
                called = true;
            }
        }
    }

    @Test
    public void testCallback() throws Exception {
        final ProblematicPostPersistEntity p = new ProblematicPostPersistEntity();
        getDs().save(p);
        Assert.assertTrue(p.called);
        Assert.assertTrue(p.i.called);
    }
}
