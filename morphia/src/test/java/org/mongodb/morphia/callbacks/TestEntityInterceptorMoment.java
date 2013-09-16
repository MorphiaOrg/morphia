package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestEntityInterceptorMoment extends TestBase {

  static class E {
    @Id
    private final ObjectId _id = new ObjectId();

    boolean called;

    @PrePersist void entityCallback() {
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

  @Test
  public void testGlobalEntityInterceptorWorksAfterEntityCallback() {
    morphia.map(E.class);
    morphia.getMapper().addInterceptor(new Interceptor());

    ds.save(new E());
  }
}
