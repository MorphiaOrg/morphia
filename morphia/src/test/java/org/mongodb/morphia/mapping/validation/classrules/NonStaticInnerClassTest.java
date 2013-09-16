package org.mongodb.morphia.mapping.validation.classrules;


import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.testutil.AssertedFailure;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NonStaticInnerClassTest extends TestBase {

  static class Valid {
    @Id ObjectId id;
  }

  class InValid {
    @Id ObjectId id;
  }

  @Test
  public void testValidInnerClass() throws Exception {
    morphia.map(Valid.class);
  }

  @Test
  public void testInValidInnerClass() throws Exception {
    new AssertedFailure(MappingException.class) {
      @Override
      protected void thisMustFail() {
        morphia.map(InValid.class);
      }
    };
  }
}
