package dev.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;


public class SortByIdTest extends TestBase {

    @Test
    public void getLastByIdTest() {
        final A a1 = new A("a1");
        final A a2 = new A("a2");
        final A a3 = new A("a3");

        getDs().save(a1);
        getDs().save(a2);
        getDs().save(a3);

        Assert.assertEquals("last id", a3.id, getDs().find(A.class).order("-id")
                                                     .find(new FindOptions().limit(1))
                                                     .next()
                                                  .id);
        Assert.assertEquals("last id", a3.id, getDs().find(A.class).disableValidation().order("-_id")
                                                     .find(new FindOptions().limit(1))
                                                     .next()
                                                  .id);
    }

    @Entity("A")
    static class A {
        @Id
        private ObjectId id;
        private String name;

        A(final String name) {
            this.name = name;
        }

        A() {
        }
    }

}
