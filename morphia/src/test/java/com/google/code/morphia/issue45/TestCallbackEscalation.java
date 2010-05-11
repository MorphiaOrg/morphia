/**
 * 
 */
package com.google.code.morphia.issue45;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.Transient;

public class TestCallbackEscalation extends AbstractMorphiaTest
{
    @Entity
    static class A extends Callbacks
    {
        @Id
        String id;

        @Embedded
        B b;

        @Embedded
        List<B> bs = new LinkedList<B>();

        String getId()
        {
            return this.id;
        }

    }

    @Embedded
    static class B extends Callbacks
    {
        // minor issue: i realized, that if B does not bring anything to map,
        // morphia behaves significantly different, is this wanted ?
        // see TestEmptyEntityMapping
        String someProperty = "someThing";
    }

    static class Callbacks
    {
        @Transient
        boolean prePersist, postPersist, preLoad, postLoad;

        @PrePersist
        void prePersist()
        {
            this.prePersist = true;
        }

        @PostPersist
        void postPersist()
        {
            this.postPersist = true;
        }

        @PreLoad
        void preLoad()
        {
            this.preLoad = true;
        }

        @PostLoad
        void postLoad()
        {
            this.postLoad = true;
        }
    }

    @Test
    public void testPrePersistEscalation() throws Exception
    {
        final A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.prePersist);
        Assert.assertFalse(a.b.prePersist);
        Assert.assertFalse(a.bs.get(0).prePersist);

        this.ds.save(a);

        Assert.assertTrue(a.prePersist);
        Assert.assertTrue(a.b.prePersist);
        Assert.assertTrue(a.bs.get(0).prePersist);
    }

    @Test
    public void testPostPersistEscalation() throws Exception
    {
        final A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.postPersist);
        Assert.assertFalse(a.b.postPersist);
        Assert.assertFalse(a.bs.get(0).postPersist);

        this.ds.save(a);

        Assert.assertTrue(a.postPersist);
        Assert.assertTrue(a.b.postPersist); // fails here
        Assert.assertTrue(a.bs.get(0).postPersist); // and here
    }

    @Test
    public void testPreLoadEscalation() throws Exception
    {
        A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.preLoad);
        Assert.assertFalse(a.b.preLoad);
        Assert.assertFalse(a.bs.get(0).preLoad);

        this.ds.save(a);

        Assert.assertFalse(a.preLoad);
        Assert.assertFalse(a.b.preLoad);
        Assert.assertFalse(a.bs.get(0).preLoad);

        a = this.ds.find(A.class, "_id", a.getId()).get();

        Assert.assertTrue(a.preLoad);
        Assert.assertTrue(a.b.preLoad);
        Assert.assertTrue(a.bs.get(0).preLoad);

    }

    @Test
    public void testPostLoadEscalation() throws Exception
    {
        A a = new A();
        a.b = new B();
        a.bs.add(new B());

        Assert.assertFalse(a.postLoad);
        Assert.assertFalse(a.b.postLoad);
        Assert.assertFalse(a.bs.get(0).postLoad);

        this.ds.save(a);

        Assert.assertFalse(a.preLoad);
        Assert.assertFalse(a.b.preLoad);
        Assert.assertFalse(a.bs.get(0).preLoad);

        a = this.ds.find(A.class, "_id", a.getId()).get();

        Assert.assertTrue(a.postLoad);
        Assert.assertTrue(a.b.postLoad);
        Assert.assertTrue(a.bs.get(0).postLoad);

    }
}
