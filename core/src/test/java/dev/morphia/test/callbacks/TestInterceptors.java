package dev.morphia.test.callbacks;

import java.util.List;

import com.mongodb.lang.NonNull;

import dev.morphia.Datastore;
import dev.morphia.PrePersistListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestInterceptors extends TestBase {

    public TestInterceptors() {
        super(buildConfig(E.class).listeners(List.of(new Interceptor())));
    }

    @Test
    public void testGlobalListenerRunsAfterEntityCallback() {
        getDs().save(new E());
    }

    @Entity
    static class E {
        @Id
        private final ObjectId id = new ObjectId();

        private boolean called;

        @PrePersist
        void entityCallback() {
            called = true;
        }
    }

    public static class Interceptor implements PrePersistListener<Object> {
        @Override
        public void prePersist(@NonNull Object ent, @NonNull Datastore datastore) {
            Assertions.assertTrue(((E) ent).called);
        }
    }
}
