package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostPersist;

import static org.junit.Assert.assertNotNull;


public class TestPostPersist extends TestBase {

    @Test
    public void testBulkTest() {
        getMorphia().setUseBulkWriteOperations(false);
        getDs().getDB().dropDatabase();

        TestObject to1 = new TestObject();
        TestObject to2 = new TestObject();
        getAds().insert(to1, to2);

        assertNotNull("normal insert1:", to1.id);
        assertNotNull("normal insert1:", to1.one);
        assertNotNull("normal insert2:", to2.id);
        assertNotNull("normal insert2:", to2.one);

        getMorphia().setUseBulkWriteOperations(true);
        getDs().getDB().dropDatabase();

        to1 = new TestObject();
        to2 = new TestObject();
        getAds().insert(to1, to2);

        assertNotNull("bulk insert1:", to1.id);
        assertNotNull("bulk insert1:", to1.one);
        assertNotNull("bulk insert2:", to2.id);
        assertNotNull("bulk insert2:", to2.one);
    }

    @Test
    public void testCallback() throws Exception {
        final ProblematicPostPersistEntity p = new ProblematicPostPersistEntity();
        getDs().save(p);
        Assert.assertTrue(p.called);
        Assert.assertTrue(p.i.called);
    }

    public static class ProblematicPostPersistEntity {
        private final Inner i = new Inner();
        @Id
        private ObjectId id;
        private boolean called;

        static class Inner {
            private boolean called;

            private String foo = "foo";

            @PostPersist
            void m2() {
                called = true;
            }
        }

        @PostPersist
        void m1() {
            called = true;
        }
    }

    @Entity
    public static class TestObject {

        @Id
        private ObjectId id;
        private String one;

        @PostPersist
        public void doIt() {
            one = "one";
        }
    }
}
