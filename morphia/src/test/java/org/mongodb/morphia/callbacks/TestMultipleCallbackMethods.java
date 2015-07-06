package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;


public class TestMultipleCallbackMethods extends TestBase {
    private static int loading;

    @Test
    public void testMultipleCallbackAnnotation() throws Exception {
        final SomeEntity entity = new SomeEntity();
        getDs().save(entity);

        Assert.assertEquals(4, entity.getFoo());
        Assert.assertEquals(0, loading);

        final SomeEntity someEntity = getDs().find(SomeEntity.class, "_id", entity.getId()).get();

        Assert.assertEquals(4, entity.getFoo());

        Assert.assertEquals(-1, someEntity.getFoo());
        Assert.assertEquals(2, loading);
    }

    abstract static class CallbackAbstractEntity {
        @Id
        private final ObjectId id = new ObjectId();
        private int foo;

        public ObjectId getId() {
            return id;
        }

        int getFoo() {
            return foo;
        }

        void setFoo(final int foo) {
            this.foo = foo;
        }

        @PrePersist
        void prePersist1() {
            foo++;
        }

        @PrePersist
        void prePersist2() {
            foo++;
        }

        @PostPersist
        void postPersist1() {
            foo++;
        }

        @PostPersist
        void postPersist2() {
            foo++;
        }

        @PreLoad
        void preLoad1() {
            loading++;
        }

        @PreLoad
        void preLoad2() {
            loading++;
        }

        @PostLoad
        void postLoad1() {
            foo--;
        }

        @PostLoad
        void postLoad2() {
            foo--;
        }

        @PostLoad
        void postLoad3() {
            foo--;
        }
    }

    static class SomeEntity extends CallbackAbstractEntity {

    }
}
