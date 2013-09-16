package org.mongodb.morphia.mapping;


import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;


public class UpdateRetainsClassInfoTest extends TestBase {
  public abstract static class E {
    @Id ObjectId id = new ObjectId();
  }

  public static class E1 extends E {
    String foo;
  }

  public static class E2 extends E {
    String bar;
  }

  public static class X {
    @Id ObjectId id;
    final Map<String, E> map = new HashMap<String, E>();

  }

  @Test
  public void retainsClassName() {
    final X x = new X();

    final E1 e1 = new E1();
    e1.foo = "narf";
    x.map.put("k1", e1);

    final E2 e2 = new E2();
    e2.bar = "narf";
    x.map.put("k2", e2);

    ds.save(x);

    final Query<X> state_query = ds.createQuery(X.class);
    final UpdateOperations<X> state_update = ds.createUpdateOperations(X.class);
    state_update.set("map.k2", e2);

    ds.update(state_query, state_update);

    // fails due to type now missing
    ds.find(X.class).get();
  }
}