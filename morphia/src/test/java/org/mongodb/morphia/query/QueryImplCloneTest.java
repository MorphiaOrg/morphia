package org.mongodb.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.util.Arrays;
import java.util.List;

public class QueryImplCloneTest extends TestBase {

    @Test
    public void testQueryClone() throws Exception {
        final Query q = getDs().createQuery(E1.class)
                               .field("i")
                               .equal(5)
                               .limit(5)
                               .filter("a", "value_a")
                               .filter("b", "value_b")
                               .offset(5)
                               .batchSize(10)
                               .disableCursorTimeout()
                               .hintIndex("a")
                               .order("a");
        q.disableValidation().filter("foo", "bar");
        Assert.assertEquals(q, q.cloneQuery());
    }

    static class E1 {
        @Id
        private ObjectId id;

        private String a;
        private String b;
        private int i;
        private E2 e2 = new E2();
    }

    static class E2 {
        private String foo;
    }
}
