package org.mongodb.morphia;


import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.testutil.TestEntity;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class TestGetByKeys extends TestBase {
  @Test
  public final void testGetByKeys() {
    final A a1 = new A();
    final A a2 = new A();

    final Iterable<Key<A>> keys = ds.save(a1, a2);

    final List<A> reloaded = ds.getByKeys(keys);

    final Iterator<A> i = reloaded.iterator();
    Assert.assertNotNull(i.next());
    Assert.assertNotNull(i.next());
    Assert.assertFalse(i.hasNext());
  }

  public static class A extends TestEntity {
    private static final long serialVersionUID = 1L;
    String foo = "bar";
  }

}
