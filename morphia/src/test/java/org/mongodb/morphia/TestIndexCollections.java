package org.mongodb.morphia;


import com.mongodb.DB;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

import static org.junit.Assert.assertEquals;


/**
 * @author Albert Choi
 */
public class TestIndexCollections extends TestBase {

    @Entity
    private static class A {
        @Id
        private ObjectId id;
        @Indexed
        private String field;
        @Property
        private String field2;
    }

    @Entity
    private static class B {
        @Id
        private ObjectId id;
        @Embedded
        private BB bb;

        @SuppressWarnings("unused")
        public B() {
        }

        public B(final BB bb) {
            this.bb = bb;
        }
    }

    @Embedded
    private static class BB {
        @Indexed
        private String name;

        @SuppressWarnings("unused")
        public BB() {
        }

        public BB(final String name) {
            this.name = name;
        }
    }

    /**
     * Test that indexes created on one collection do not affect other collections.
     */
    @Test
    public void testA() {
        AdvancedDatastore ads = getAds();
        DB db = getDb();

        ads.ensureIndexes("a_1", A.class);
        assertEquals(2, db.getCollection("a_1").getIndexInfo().size());

        ads.ensureIndex("a_2", A.class, "field2");
        ads.ensureIndex("a_2", A.class, "-field2");
        ads.ensureIndexes("a_2", A.class);
        assertEquals(4, db.getCollection("a_2").getIndexInfo().size());

        ads.ensureIndex("a_3", A.class, "field, field2");
        assertEquals(2, db.getCollection("a_3").getIndexInfo().size());

        ads.ensureIndexes();
        assertEquals(2, db.getCollection("a_1").getIndexInfo().size());
        assertEquals(4, db.getCollection("a_2").getIndexInfo().size());
        assertEquals(2, db.getCollection("a_3").getIndexInfo().size());
    }

    @Test
    public void testEmbedded() {
        AdvancedDatastore ads = getAds();
        DB db = getDb();

        // Make collection, but not the index
        ads.save("b_1", new B(new BB("test")));
        assertEquals(1, db.getCollection("b_1").getIndexInfo().size());

        ads.ensureIndexes("b_2", B.class);
        assertEquals(2, db.getCollection("b_2").getIndexInfo().size());
    }

    protected void cleanup() {
        super.cleanup();
        DB db = getDb();
        db.getCollection("a_1").drop();
        db.getCollection("a_2").drop();
        db.getCollection("a_3").drop();
        db.getCollection("b_1").drop();
        db.getCollection("b_2").drop();
    }
}
