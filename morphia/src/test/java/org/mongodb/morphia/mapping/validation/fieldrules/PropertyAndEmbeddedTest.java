package org.mongodb.morphia.mapping.validation.fieldrules;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.mongodb.morphia.testutil.TestEntity;
import com.mongodb.DBObject;
import org.junit.Assert;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class PropertyAndEmbeddedTest extends TestBase {
  public static class E extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Embedded("myFunkyR") R r = new R();

    @PreSave
    public void preSave(final DBObject o) {
      document = o.toString();
      //			System.out.println(document);
    }

    @Transient String document;
  }

  public static class E2 extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Embedded @Property("myFunkyR") String s;
  }

  public static class R {
    String foo = "bar";
  }

  @Test
  public void testCheck() {

    final E e = new E();
    ds.save(e);

    Assert.assertTrue(e.document.contains("myFunkyR"));

    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(E2.class);
      }
    };
  }
}
