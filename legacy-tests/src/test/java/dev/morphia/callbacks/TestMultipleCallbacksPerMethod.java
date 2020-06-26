package dev.morphia.callbacks;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.Transient;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;


public class TestMultipleCallbacksPerMethod extends TestBase {
    @Test
    public void testMultipleCallbackAnnotation() {
        final SomeEntity entity = new SomeEntity();
        Assert.assertFalse(entity.isPersistent());
        getDs().save(entity);
        Assert.assertTrue(entity.isPersistent());
        final SomeEntity reloaded = getDs().find(SomeEntity.class)
                                           .filter(eq("id", entity.getId())).iterator(new FindOptions().limit(1))
                                           .tryNext();
        Assert.assertTrue(reloaded.isPersistent());
    }

    @Entity
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
