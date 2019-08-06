package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;


public class ConcreteClassEmbeddedOverrideTest extends TestBase {

    @Test
    public void test() throws Exception {
        final E e1 = new E();
        Assert.assertEquals("A", e1.a1.s);
        Assert.assertEquals("A", e1.a2.s);

        getDs().save(e1);

        final E e2 = getDs().get(e1);

        Assert.assertEquals("A", e2.a1.s);
        Assert.assertEquals("A", e2.a2.s);
        Assert.assertEquals(B.class, e2.a2.getClass());
        Assert.assertEquals(A.class, e2.a1.getClass());

    }

    @Entity
    public static class E {
        @Id
        private ObjectId id;
        private final A a1 = new A();
        @Property(concreteClass = B.class)
        private final A a2 = new A();
    }

    @Embedded
    public static class A {
        private String s = "A";

        public String getS() {
            return s;
        }

        public void setS(final String s) {
            this.s = s;
        }
    }

    public static class B extends A {
        public B() {
            setS("B");
        }
    }
}
