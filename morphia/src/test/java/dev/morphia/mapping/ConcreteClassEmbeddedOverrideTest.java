package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;


public class ConcreteClassEmbeddedOverrideTest extends TestBase {

    @Test
    public void test() {
        getMapper().map(E.class);
        final E e1 = new E();
        Assert.assertEquals("A", e1.a1.s);
        Assert.assertEquals("B", e1.a2.s);

        getDs().save(e1);

        final Datastore datastore = getDs();
        final E e2 = datastore.find(E.class)
                              .filter("_id", e1.id)
                              .first();

        Assert.assertEquals("A", e2.a1.s);
        Assert.assertEquals("B", e2.a2.s);
        Assert.assertEquals(B.class, e2.a2.getClass());
        Assert.assertEquals(A.class, e2.a1.getClass());

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

    @Entity
    public static class E {
        private final A a1 = new A();
        @Property(concreteClass = B.class)
        private final A a2 = new B();
        @Id
        private ObjectId id;
    }
}
