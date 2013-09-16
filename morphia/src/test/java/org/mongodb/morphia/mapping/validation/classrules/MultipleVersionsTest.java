package org.mongodb.morphia.mapping.validation.classrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MultipleVersionsTest extends TestBase {

  public static class Fail1 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version long v1;
    @Version long v2;
  }

  public static class OK1 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version long v1;
  }

  @Test
  public void testCheck() {
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(Fail1.class);
      }
    };
    morphia.map(OK1.class);
  }

}
