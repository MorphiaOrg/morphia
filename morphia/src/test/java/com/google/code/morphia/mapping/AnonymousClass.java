package com.google.code.morphia.mapping;


import java.io.Serializable;

import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.AdvancedDatastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import junit.framework.Assert;


/**
 * @author scott hernandez
 */
public class AnonymousClass extends TestBase {

  @Embedded
  private static class CId implements Serializable {
    static final long serialVersionUID = 1L;
    final ObjectId id = new ObjectId();
    String name;

    CId() {
    }

    CId(final String n) {
      name = n;
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof CId)) {
        return false;
      }
      final CId other = ((CId) obj);
      return other.id.equals(id) && other.name.equals(name);
    }

  }

  private static class E {
    @Id CId id;
    String e;
  }


  @Test
  public void testMapping() throws Exception {
    E e = new E();
    e.id = new CId("test");

    ds.save(e);
    e = ds.get(e);
    Assert.assertEquals("test", e.id.name);
    Assert.assertNotNull(e.id.id);
  }

  @Test
  public void testDelete() throws Exception {
    final E e = new E();
    e.id = new CId("test");

    final Key<E> key = ds.save(e);
    ds.delete(E.class, e.id);
  }

  @Test
  public void testOtherDelete() throws Exception {
    final E e = new E();
    e.id = new CId("test");

    ds.save(e);
    ((AdvancedDatastore) ds).delete(ds.getCollection(E.class).getName(), E.class, e.id);
  }

}
