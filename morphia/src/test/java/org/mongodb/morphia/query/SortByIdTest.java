package org.mongodb.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;


public class SortByIdTest extends TestBase {

    @Entity("A")
    static class A {
        @Id
        ObjectId id;
        String name;

        public A(final String name){
            this.name=name;
        }
        public A(){}
    }

    @Test
    public void getLastByIdTest(){
        final A a1 = new A("a1");
        final A a2 = new A("a2");
        final A a3 = new A("a3");

        ds.save(a1);
        ds.save(a2);
        ds.save(a3);

        Assert.assertEquals("last id", a3.id, ds.createQuery(A.class).order("-id").get().id);
        Assert.assertEquals("last id", a3.id, ds.createQuery(A.class).disableValidation().order("-_id").get().id);
    }

}