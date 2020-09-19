package dev.morphia.callbacks;


import dev.morphia.EntityInterceptor;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;


public class TestEntityInterceptorMoment extends TestBase {

    @Test
    public void testGlobalEntityInterceptorWorksAfterEntityCallback() {
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
        public void prePersist(Object ent, Document document, Mapper mapper) {
            Assert.assertTrue(((E) ent).called);
        }
    }
}
