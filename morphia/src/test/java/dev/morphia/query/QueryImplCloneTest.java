package dev.morphia.query;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;

public class QueryImplCloneTest extends TestBase {

    @Test
    @SuppressWarnings("deprecation")
    public void testQueryCloneOld() throws Exception {
        final Query q = getDs().find(E1.class)
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

    @Test
    public void testQueryClone() throws Exception {
        final Query q = getDs().find(E1.class)
                               .field("i")
                               .equal(5)
                               .filter("a", "value_a")
                               .filter("b", "value_b")
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
