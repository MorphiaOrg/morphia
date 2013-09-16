package org.mongodb.morphia.query;


import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.testutil.AssertedFailure;


public class TestMandatoryId extends TestBase {

  @Entity
  public static class E {
    // not id here
    String foo = "bar";
  }

  @Test
  public final void testMissingId() {
    new AssertedFailure() {

      @Override
      protected void thisMustFail() {
        morphia.map(E.class);
      }
    };
  }

  @Test
  public final void testMissingIdNoImplicitMapCall() {
    final Key<E> save = ds.save(new E());

    new AssertedFailure() {
      @Override
      protected void thisMustFail() {
        ds.getByKey(E.class, save);
      }
    };
  }

}
