package org.mongodb.morphia.issue325;


import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;
import org.junit.Assert;


public class TestEmbeddedClassname extends TestBase {

  //	@SuppressWarnings("unused")
  @Entity(noClassnameStored = true)
  private static class Root {
    @Id String id = "a";

    @Embedded
    final List<A> as = new ArrayList<A>();

    @Embedded
    final List<B> bs = new ArrayList<B>();
  }

  private static class A {
    String name = "undefined";

    @Transient DBObject raw;

    @PreLoad void preLoad(final DBObject dbObj) {
      raw = dbObj;
    }
  }

  private static class B extends A {
    String description = "<description here>";
  }

  @Test
  public final void testEmbeddedClassname() {
    Root r = new Root();
    ds.save(r);

    final A a = new A();
    ds.update(ds.createQuery(Root.class), ds.createUpdateOperations(Root.class).add("as", a));
    r = ds.get(Root.class, "a");
    Assert.assertFalse(r.as.get(0).raw.containsField(Mapper.CLASS_NAME_FIELDNAME));

    B b = new B();
    ds.update(ds.createQuery(Root.class), ds.createUpdateOperations(Root.class).add("bs", b));
    r = ds.get(Root.class, "a");
    Assert.assertFalse(r.bs.get(0).raw.containsField(Mapper.CLASS_NAME_FIELDNAME));

    ds.delete(ds.createQuery(Root.class));
    //test saving an B in as, and it should have the classname.

    ds.save(new Root());
    b = new B();
    ds.update(ds.createQuery(Root.class), ds.createUpdateOperations(Root.class).add("as", b));
    r = ds.get(Root.class, "a");
    Assert.assertTrue(r.as.get(0).raw.containsField(Mapper.CLASS_NAME_FIELDNAME));

  }

}
