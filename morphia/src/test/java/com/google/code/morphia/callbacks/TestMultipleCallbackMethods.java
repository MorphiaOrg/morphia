package com.google.code.morphia.callbacks;


import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PrePersist;
import junit.framework.Assert;


public class TestMultipleCallbackMethods extends TestBase {
  abstract static class CallbackAbstractEntity {
    @Id
    private final String _id = new ObjectId().toStringMongod();

    public String getId() {
      return _id;
    }

    int foo;

    @PrePersist void prePersist1() {
      foo++;
    }

    @PrePersist void prePersist2() {
      foo++;
    }
  }

  static class SomeEntity extends CallbackAbstractEntity {

  }

  @Test
  public void testMultipleCallbackAnnotation() throws Exception {
    final SomeEntity entity = new SomeEntity();
    ds.save(entity);
    Assert.assertEquals(2, entity.foo);
  }
}