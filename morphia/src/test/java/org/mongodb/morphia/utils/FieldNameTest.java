package org.mongodb.morphia.utils;


import org.junit.Test;
import org.mongodb.morphia.testutil.AssertedFailure;
import org.junit.Assert;


public class FieldNameTest {

  private String foo;
  private String bar;

  @Test
  public void testFieldNameOf() throws Exception {
    Assert.assertEquals("foo", FieldName.of("foo"));
    Assert.assertEquals("bar", FieldName.of("bar"));
    new AssertedFailure(FieldName.FieldNameNotFoundException.class) {

      @Override
      protected void thisMustFail() {
        FieldName.of("buh");
      }
    };
    Assert.assertEquals("x", FieldName.of(E2.class, "x"));
    Assert.assertEquals("y", FieldName.of(E2.class, "y"));
  }
}

class E1 {
  private final int x = 0;
}

class E2 extends E1 {
  private final int y = 0;
}
