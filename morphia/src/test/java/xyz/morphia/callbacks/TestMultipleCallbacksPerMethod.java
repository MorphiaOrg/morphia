package xyz.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.PostLoad;
import xyz.morphia.annotations.PostPersist;
import xyz.morphia.annotations.Transient;
import xyz.morphia.query.FindOptions;


public class TestMultipleCallbacksPerMethod extends TestBase {
    @Test
    public void testMultipleCallbackAnnotation() {
        final SomeEntity entity = new SomeEntity();
        Assert.assertFalse(entity.isPersistent());
        getDs().save(entity);
        Assert.assertTrue(entity.isPersistent());
        final SomeEntity reloaded = getDs().find(SomeEntity.class).filter("id", entity.getId())
                                           .find(new FindOptions().limit(1))
                                           .tryNext();
        Assert.assertTrue(reloaded.isPersistent());
    }

    abstract static class CallbackAbstractEntity {
        @Id
        private final String id = new ObjectId().toHexString();
        @Transient
        private boolean persistentMarker;

        public String getId() {
            return id;
        }

        public boolean isPersistent() {
            return persistentMarker;
        }

        @PostPersist
        @PostLoad
        void markPersistent() {
            persistentMarker = true;
        }
    }

    static class SomeEntity extends CallbackAbstractEntity {

    }
}
