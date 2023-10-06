package dev.morphia.test.callbacks;

import java.lang.annotation.Annotation;

import com.mongodb.lang.NonNull;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestInterceptors extends TestBase {

    public TestInterceptors() {
        super(buildConfig(E.class));
    }

    @Test
    @SuppressWarnings("removal")
    public void testGlobalInterceptor() {
        getMapper().addInterceptor(new Interceptor());

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

    public static class Interceptor implements EntityListener<Object> {
        @Override
        public boolean hasAnnotation(@NonNull Class<? extends Annotation> type) {
            return PrePersist.class.equals(type);
        }

        @Override
        public void prePersist(@NonNull Object ent, @NonNull Document document, @NonNull Datastore datastore) {
            Assert.assertTrue(((E) ent).called);
        }
    }
}
