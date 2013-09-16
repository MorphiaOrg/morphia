package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReferenceAndSerializableTest extends TestBase {
  public static class E extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Reference @Serialized R r;
  }

  public static class R extends TestEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
  }

  @Test
  public void testCheck() {
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(E.class);
      }

      @Override
      protected boolean dumpToSystemOut() {
        return true;
      }
    };
  }
}
