package dev.morphia.test.callbacks;


import com.mongodb.lang.NonNull;
import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestInterceptors extends TestBase {

    @Test
    public void testGlobalInterceptor() {
        getMapper().map(E.class);
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

    public static class Interceptor implements EntityInterceptor {
        @Override
        public void prePersist(@NonNull Object ent, @NonNull Document document, @NonNull Mapper mapper) {
            Assert.assertTrue(((E) ent).called);
        }
    }
}
