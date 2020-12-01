package dev.morphia.callbacks;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Transient;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;


public class TestCallbackEscalation extends TestBase {
    @Test
    public void testPostLoadEscalation() {
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

        a = getDs().find(A.class)
                   .filter(eq("_id", a.id)).iterator(new FindOptions().limit(1))
                   .tryNext();

        Assert.assertTrue(a.isPostLoad());
        Assert.assertTrue(a.b.isPostLoad());
        Assert.assertTrue(a.bs.get(0).isPostLoad());

    }

    @Test
    public void testPostPersistEscalation() {
        final A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.isPostPersist());
        Assert.assertFalse(a.b.isPostPersist());
        Assert.assertFalse(a.bs.get(0).isPostPersist());

        getDs().save(a);

        Assert.assertTrue(a.isPostPersist());
        Assert.assertTrue(a.b.isPostPersist()); //PostPersist in not only called on entities
        Assert.assertTrue(a.bs.get(0).isPostPersist()); //PostPersist is not only called on entities
    }

    @Test
    public void testPreLoadEscalation() {
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

        a = getDs().find(A.class)
                   .filter(eq("_id", a.id)).iterator(new FindOptions().limit(1))
                   .tryNext();

        Assert.assertTrue(a.isPreLoad());
        Assert.assertTrue(a.b.isPreLoad());
        Assert.assertTrue(a.bs.get(0).isPreLoad());

    }

    @Test
    public void testPrePersistEscalation() {
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
        @Id
        private ObjectId id;
        private final List<B> bs = new LinkedList<>();
        private B b;
    }

    @Entity
    static class B extends Callbacks {
        // minor issue: i realized, that if B does not bring anything to map,
        // morphia behaves significantly different, is this wanted ?
        // see TestEmptyEntityMapping
        private final String someProperty = "someThing";
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
    }
}
