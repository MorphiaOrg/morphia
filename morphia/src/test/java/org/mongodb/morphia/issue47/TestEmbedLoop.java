package org.mongodb.morphia.issue47;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.testutil.TestEntity;


public class TestEmbedLoop extends TestBase {

  @Entity
  static class A extends TestEntity {
    private static final long serialVersionUID = 1L;
    @Embedded B b;
  }

  @Embedded
  static class B extends TestEntity {
    private static final long serialVersionUID = 1L;
    String someProperty = "someThing";

    // produces stack overflow, might be detectable?
    // @Reference this would be right way to do it.

    @Embedded A a;
  }

  @Test @Ignore
  public void testCircularRefs() throws Exception {

    morphia.map(A.class);

    A a = new A();
    a.b = new B();
    a.b.a = a;

    Assert.assertSame(a, a.b.a);

    ds.save(a);
    a = ds.find(A.class, "_id", a.getId()).get();
    Assert.assertSame(a, a.b.a);
  }
}