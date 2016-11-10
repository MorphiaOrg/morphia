package org.mongodb.morphia.issue49;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.testutil.TestEntity;

import static java.util.Arrays.asList;


public class TestReferenceArray extends TestBase {

    @Test
    public final void testArrayPersistence() {
        A a = new A();
        final B b1 = new B();
        final B b2 = new B();

        a.bs[0] = b1;
        a.bs[1] = b2;

        getDs().save(asList(b2, b1, a));

        getDs().get(a);
    }


    public static class A extends TestEntity {
        @Reference
        private final B[] bs = new B[2];
    }

    public static class B extends TestEntity {
        private String foo;
    }

}
