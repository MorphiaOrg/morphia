package org.mongodb.morphia.issue50;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

import static org.junit.Assert.fail;


public class TestIdTwice extends TestBase {

  @Test
  public final void testRedundantId() {
    try {
      morphia.map(A.class);
      fail();
    } catch (ConstraintViolationException expected) {
      // fine
    }
  }

  public static class A extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Id String extraId;
    @Id String broken;
  }

}
