package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;

import java.util.HashMap;
import java.util.Map;


public class UpdateRetainsClassInfoTest extends TestBase {
    @Test
    public void retainsClassName() {
        final X x = new X();

        final E1 e1 = new E1();
        e1.foo = "narf";
        x.map.put("k1", e1);

        final E2 e2 = new E2();
        e2.bar = "narf";
        x.map.put("k2", e2);

        getDs().save(x);

        final Query<X> query = getDs().find(X.class);
        final UpdateOperations<X> update = getDs().createUpdateOperations(X.class);
        update.set("map.k2", e2);

        getDs().update(query, update);

        // fails due to type now missing
        getDs().find(X.class)
               .find(new FindOptions().limit(1))
               .next();
    }

    public abstract static class E {
        @Id
        private ObjectId id = new ObjectId();
    }

    public static class E1 extends E {
        private String foo;
    }

    public static class E2 extends E {
        private String bar;
    }

    public static class X {
        private final Map<String, E> map = new HashMap<String, E>();
        @Id
        private ObjectId id;

    }
}
