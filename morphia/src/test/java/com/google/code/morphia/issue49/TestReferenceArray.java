package com.google.code.morphia.issue49;


import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.testutil.TestEntity;


public class TestReferenceArray extends TestBase {

  @Test
  public final void testArrayPersistence() {
    A a = new A();
    final B b1 = new B();
    final B b2 = new B();

    a.bs[0] = b1;
    a.bs[1] = b2;

    ds.save(b2, b1, a);

    ds.get(a);
  }


  public static class A extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Reference
    final B[] bs = new B[2];
  }

  public static class B extends TestEntity {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    String foo;
  }

}
