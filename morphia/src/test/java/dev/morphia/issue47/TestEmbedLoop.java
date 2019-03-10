package dev.morphia.issue47;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.testutil.TestEntity;


public class TestEmbedLoop extends TestBase {

    @Test
    @Ignore
    public void testCircularRefs() throws Exception {

        getMorphia().map(A.class);

        A a = new A();
        a.b = new B();
        a.b.a = a;

        Assert.assertSame(a, a.b.a);

        getDs().save(a);
        a = getDs().find(A.class).filter("_id", a.getId()).get();
        Assert.assertSame(a, a.b.a);
    }

    @Entity
    static class A extends TestEntity {
        @Embedded
        private B b;
    }

    @Embedded
    static class B extends TestEntity {
        private String someProperty = "someThing";

        // produces stack overflow, might be detectable?
        // @Reference this would be right way to do it.

        @Embedded
        private A a;
    }
}
