package com.google.code.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import junit.framework.Assert;


public class TestMultipleCallbackMethods extends TestBase {
  private static int loading;

  abstract static class CallbackAbstractEntity {
    @Id
    private final ObjectId _id = new ObjectId();

    public ObjectId getId() {
      return _id;
    }

    int foo;

    @PrePersist
    void prePersist1() {
      foo++;
    }

    @PrePersist
    void prePersist2() {
      foo++;
    }

    @PostPersist
    void postPersist1() {
      foo++;
    }

    @PostPersist
    void postPersist2() {
      foo++;
    }

    @PreLoad
    void preLoad1() {
      loading++;
    }

    @PreLoad
    void preLoad2() {
      loading++;
    }

    @PostLoad
    void postLoad1() {
      foo--;
    }

    @PostLoad
    void postLoad2() {
      foo--;
    }

    @PostLoad
    void postLoad3() {
      foo--;
    }
  }

  static class SomeEntity extends CallbackAbstractEntity {

  }

  @Test
  public void testMultipleCallbackAnnotation() throws Exception {
    final SomeEntity entity = new SomeEntity();
    ds.save(entity);

    Assert.assertEquals(4, entity.foo);
    Assert.assertEquals(0, loading);

    final SomeEntity someEntity = ds.find(SomeEntity.class, "_id", entity.getId()).get();

    Assert.assertEquals(4, entity.foo);

    Assert.assertEquals(-1, someEntity.foo);
    Assert.assertEquals(2, loading);
  }
}