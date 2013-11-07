package org.mongodb.morphia.issue45;


import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.testutil.TestEntity;


public class TestEmptyEntityMapping extends TestBase {
    @Entity
    static class A extends TestEntity {
        @Embedded
        private B b;
    }

    @Embedded
    static class B {
        @Transient
        private String foo;
    }

    @Test
    public void testEmptyEmbeddedNotNullAfterReload() throws Exception {
        A a = new A();
        a.b = new B();

        getDs().save(a);
        Assert.assertNotNull(a.b);

        a = getDs().find(A.class, "_id", a.getId()).get();
        Assert.assertNull(a.b);
    }
}
