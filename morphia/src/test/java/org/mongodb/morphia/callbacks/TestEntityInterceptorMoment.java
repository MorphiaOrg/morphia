package org.mongodb.morphia.callbacks;


import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.mapping.Mapper;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestEntityInterceptorMoment extends TestBase {

    @Test
    public void testGlobalEntityInterceptorWorksAfterEntityCallback() {
        getMorphia().map(E.class);
        getMorphia().getMapper().addInterceptor(new Interceptor());

        getDs().save(new E());
    }

    static class E {
        @Id
        private final ObjectId id = new ObjectId();

        private boolean called;

        @PrePersist
        void entityCallback() {
            called = true;
        }
    }

    public static class Interceptor extends AbstractEntityInterceptor {
        @Override
        public void postLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void postPersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void preLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
            Assert.assertTrue(((E) ent).called);
        }

        @Override
        public void preSave(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

    }
}
