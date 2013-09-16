package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class VersionMisuseTest extends TestBase {

  public static class Fail1 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version long hubba = 1;
  }

  public static class Fail2 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version Long hubba = 1L;
  }

  public static class OK1 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version long hubba;
  }

  public static class OK2 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Version long hubba;
  }

  @Test
  public void testCheck() {
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(Fail1.class);
      }
    };
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(Fail2.class);
      }
    };
    morphia.map(OK1.class);
    morphia.map(OK2.class);

  }

}
