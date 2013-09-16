package org.mongodb.morphia.mapping.validation.classrules;


import java.util.Map;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.AssertedFailure;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class DuplicatePropertyNameTest extends TestBase {
  @Entity
  public static class DuplicatedPropertyName {
    @Id String id;

    @Property(value = "value") String content1;
    @Property(value = "value") String content2;
  }

  @Entity
  public static class DuplicatedPropertyName2 {
    @Id String id;

    @Embedded(value = "value") Map<String, Integer> content1;
    @Property(value = "value") String               content2;
  }

  @Entity
  public static class Super {
    String foo;
  }

  public static class Extends extends Super {
    String foo;
  }


  @Test
  public void testDuplicatedPropertyName() throws Exception {
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(DuplicatedPropertyName.class);
      }
    };
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(DuplicatedPropertyName2.class);
      }
    };
    new AssertedFailure(ConstraintViolationException.class) {
      @Override
      public void thisMustFail() {
        morphia.map(Extends.class);
      }
    };
  }

}
