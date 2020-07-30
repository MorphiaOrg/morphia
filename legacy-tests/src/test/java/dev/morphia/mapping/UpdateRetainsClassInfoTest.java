package dev.morphia.mapping;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static dev.morphia.query.experimental.updates.UpdateOperators.set;


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
        query.update(set("map.k2", e2))
             .execute();


        // fails due to type now missing
        getDs().find(X.class).iterator(new FindOptions().limit(1))
               .next();
    }

    @Entity
    public abstract static class E {
        @Id
        private final ObjectId id = new ObjectId();
    }

    public static class E1 extends E {
        private String foo;
    }

    public static class E2 extends E {
        private String bar;
    }

    @Entity
    public static class X {
        @Id
        private ObjectId id;
        private final Map<String, E> map = new HashMap<String, E>();

    }
}
