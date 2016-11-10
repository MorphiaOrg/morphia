package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Transient;

import java.util.LinkedList;
import java.util.List;


public class TestCallbackEscalation extends TestBase {
    @Test
    public void testPostLoadEscalation() throws Exception {
        A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.isPostLoad());
        Assert.assertFalse(a.b.isPostLoad());
        Assert.assertFalse(a.bs.get(0).isPostLoad());

        getDs().save(a);

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        a = getDs().find(A.class).filter("_id", a.id).get();

        Assert.assertTrue(a.isPostLoad());
        Assert.assertTrue(a.b.isPostLoad());
        Assert.assertTrue(a.bs.get(0).isPostLoad());

    }

    @Test
    public void testPostPersistEscalation() throws Exception {
        final A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.isPostPersist());
        Assert.assertFalse(a.b.isPostPersist());
        Assert.assertFalse(a.bs.get(0).isPostPersist());

        getDs().save(a);

        Assert.assertTrue(a.isPreSave());
        Assert.assertTrue(a.isPostPersist());
        Assert.assertTrue(a.b.isPreSave());
        Assert.assertTrue(a.b.isPostPersist()); //PostPersist in not only called on entities
        Assert.assertTrue(a.bs.get(0).isPreSave());
        Assert.assertTrue(a.bs.get(0).isPostPersist()); //PostPersist is not only called on entities
    }

    @Test
    public void testPreLoadEscalation() throws Exception {
        A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        getDs().save(a);

        Assert.assertFalse(a.isPreLoad());
        Assert.assertFalse(a.b.isPreLoad());
        Assert.assertFalse(a.bs.get(0).isPreLoad());

        a = getDs().find(A.class).filter("_id", a.id).get();

        Assert.assertTrue(a.isPreLoad());
        Assert.assertTrue(a.b.isPreLoad());
        Assert.assertTrue(a.bs.get(0).isPreLoad());

    }

    @Test
    public void testPrePersistEscalation() throws Exception {
        final A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.isPrePersist());
        Assert.assertFalse(a.b.isPrePersist());
        Assert.assertFalse(a.bs.get(0).isPrePersist());

        getDs().save(a);

        Assert.assertTrue(a.isPrePersist());
        Assert.assertTrue(a.b.isPrePersist());
        Assert.assertTrue(a.bs.get(0).isPrePersist());
    }

    @Entity
    static class A extends Callbacks {
        @Embedded
        private final List<B> bs = new LinkedList<B>();
        @Id
        private ObjectId id;
        @Embedded
        private B b;
    }

    @Embedded
    static class B extends Callbacks {
        // minor issue: i realized, that if B does not bring anything to map,
        // morphia behaves significantly different, is this wanted ?
        // see TestEmptyEntityMapping
        private String someProperty = "someThing";
    }

    static class Callbacks {
        @Transient
        private boolean prePersist;
        @Transient
        private boolean postPersist;
        @Transient
        private boolean preLoad;
        @Transient
        private boolean postLoad;
        @Transient
        private boolean preSave;

        @PrePersist
        void prePersist() {
            prePersist = true;
        }

        @PostPersist
        void postPersist() {
            postPersist = true;
        }

        @PreLoad
        void preLoad() {
            preLoad = true;
        }

        @PostLoad
        void postLoad() {
            postLoad = true;
        }

        @PreSave
        void preSave() {
            preSave = true;
        }

        boolean isPostLoad() {
            return postLoad;
        }

        boolean isPostPersist() {
            return postPersist;
        }

        boolean isPreLoad() {
            return preLoad;
        }

        boolean isPrePersist() {
            return prePersist;
        }

        boolean isPreSave() {
            return preSave;
        }
    }
}
