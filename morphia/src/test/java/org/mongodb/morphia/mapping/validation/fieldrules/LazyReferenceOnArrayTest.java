package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class LazyReferenceOnArrayTest extends TestBase {

  public static class LazyOnArray extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Reference(lazy = true) R[] r;
  }

  public static class R extends TestEntity {
    private static final long serialVersionUID = 1L;
  }

  @Test
  public void testLazyRefOnArray() {
    new AssertedFailure(ConstraintViolationException.class) {

      @Override
      protected void thisMustFail() {
        morphia.map(LazyOnArray.class);
      }
    };
  }
}
