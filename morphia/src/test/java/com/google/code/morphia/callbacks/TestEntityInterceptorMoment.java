package com.google.code.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.AbstractEntityInterceptor;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.mapping.Mapper;
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
