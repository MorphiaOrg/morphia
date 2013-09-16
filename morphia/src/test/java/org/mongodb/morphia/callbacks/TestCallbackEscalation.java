package org.mongodb.morphia.callbacks;


import java.util.LinkedList;
import java.util.List;

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


public class TestCallbackEscalation extends TestBase {
  @Entity
  static class A extends Callbacks {
    @Id
    ObjectId id;

    @Embedded
    B b;

    @Embedded
    final List<B> bs = new LinkedList<B>();
  }

  @Embedded
  static class B extends Callbacks {
    // minor issue: i realized, that if B does not bring anything to map,
    // morphia behaves significantly different, is this wanted ?
    // see TestEmptyEntityMapping
    String someProperty = "someThing";
  }

  static class Callbacks {
    @Transient
    boolean prePersist;
    @Transient
    boolean postPersist;
    @Transient
    boolean preLoad;
    @Transient
    boolean postLoad;
    @Transient
    boolean preSave;

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
  }

  @Test
  public void testPrePersistEscalation() throws Exception {
    final A a = new A();
    a.b = new B();
    a.bs.add(new B());

    Assert.assertFalse(a.prePersist);
    Assert.assertFalse(a.b.prePersist);
    Assert.assertFalse(a.bs.get(0).prePersist);

    ds.save(a);

    Assert.assertTrue(a.prePersist);
    Assert.assertTrue(a.b.prePersist);
    Assert.assertTrue(a.bs.get(0).prePersist);
  }

  @Test
  public void testPostPersistEscalation() throws Exception {
    final A a = new A();
    a.b = new B();
    a.bs.add(new B());

    Assert.assertFalse(a.postPersist);
    Assert.assertFalse(a.b.postPersist);
    Assert.assertFalse(a.bs.get(0).postPersist);

    ds.save(a);

    Assert.assertTrue(a.preSave);
    Assert.assertTrue(a.postPersist);
    Assert.assertTrue(a.b.preSave);
    Assert.assertTrue(a.b.postPersist); //PostPersist in not only called on entities
    Assert.assertTrue(a.bs.get(0).preSave);
    Assert.assertTrue(a.bs.get(0).postPersist); //PostPersist is not only called on entities
  }

  @Test
  public void testPreLoadEscalation() throws Exception {
    A a = new A();
    a.b = new B();
    a.bs.add(new B());

    Assert.assertFalse(a.preLoad);
    Assert.assertFalse(a.b.preLoad);
    Assert.assertFalse(a.bs.get(0).preLoad);

    ds.save(a);

    Assert.assertFalse(a.preLoad);
    Assert.assertFalse(a.b.preLoad);
    Assert.assertFalse(a.bs.get(0).preLoad);

    a = ds.find(A.class, "_id", a.id).get();

    Assert.assertTrue(a.preLoad);
    Assert.assertTrue(a.b.preLoad);
    Assert.assertTrue(a.bs.get(0).preLoad);

  }

  @Test
  public void testPostLoadEscalation() throws Exception {
    A a = new A();
    a.b = new B();
    a.bs.add(new B());

    Assert.assertFalse(a.postLoad);
    Assert.assertFalse(a.b.postLoad);
    Assert.assertFalse(a.bs.get(0).postLoad);

    ds.save(a);

    Assert.assertFalse(a.preLoad);
    Assert.assertFalse(a.b.preLoad);
    Assert.assertFalse(a.bs.get(0).preLoad);

    a = ds.find(A.class, "_id", a.id).get();

    Assert.assertTrue(a.postLoad);
    Assert.assertTrue(a.b.postLoad);
    Assert.assertTrue(a.bs.get(0).postLoad);

  }
}
