package org.mongodb.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;


/**
 * @author doc
 */
public class QueryImplCloneTest extends TestBase {
    private static final Logger LOG = MorphiaLoggerFactory.get(QueryImplCloneTest.class);

    private static final List<String> ALLOWED = Arrays.asList("cache", "query");

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
        Assert.assertTrue(sameState(q, q.cloneQuery()));
    }

    private boolean sameState(final Query q1, final Query q2) throws IllegalAccessException {
        return sameState(q1.getClass(), q1, q2);
    }

    private boolean sameState(final Class c, final Query q1, final Query q2) throws IllegalAccessException {

        final Field[] fields = c.getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);

            final Object v1 = f.get(q1);
            final Object v2 = f.get(q2);

            LOG.debug("checking field " + f.getName() + " v1=" + v1 + " v2=" + v2);

            if (v1 == null && v2 == null) {
                continue;
            }

            if (v1 != null && v1.equals(v2)) {
                continue;
            }

            if (!ALLOWED.contains(f.getName())) {
                throw new RuntimeException(f.getName() + " v1=" + v1 + " v2=" + v2);
            }
        }

        final Class superclass = c.getSuperclass();
        return (superclass == null || sameState(superclass, q1, q2));
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
