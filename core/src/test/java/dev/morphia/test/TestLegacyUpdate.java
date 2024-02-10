package dev.morphia.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mongodb.client.result.UpdateResult;

import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.PushOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings("removal")
public class TestLegacyUpdate extends TestBase {

    private List<ObjectId> testObjectIds = List.of(ObjectId.get(), ObjectId.get());

    private EmbeddedDocument testEmbeddedDoc1 = new EmbeddedDocument(
            "foo1",
            "bar1",
            "baz1",
            testObjectIds);

    private EmbeddedDocument testEmbeddedDoc2 = new EmbeddedDocument(
            "foo2",
            "bar2",
            "baz2",
            testObjectIds);

    public TestLegacyUpdate() {
        super(buildConfig(MyDocument.class, EmbeddedDocument.class)
                .legacy());
    }

    @Test
    public void testAddToSet() {
        createTestDocuments();

        ObjectId toAdd = ObjectId.get();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .addToSet("idSet", toAdd);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument updated = findMyDocument();
        Assert.assertTrue(updated.idSet.contains(toAdd));
    }

    @Test
    public void testAddListToSet() {
        createTestDocuments();

        List<ObjectId> toAdd = List.of(ObjectId.get(), ObjectId.get());

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .addToSet("idSet", toAdd);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument updated = findMyDocument();
        for (ObjectId added : toAdd) {
            Assert.assertTrue(updated.idSet.contains(added));
        }
    }

    @Test
    public void testDec() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .dec("intField");

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, 0);
    }

    @Test
    public void testDecNumber() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .dec("intField", new Long(2));

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, -1);
    }

    @Test
    public void testInc() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .inc("intField");

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, 2);
    }

    @Test
    public void testMax() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .max("intField", 9);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, 9);
    }

    @Test
    public void testMin() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .min("intField", -1);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, -1);
    }

    @Test
    public void testPush() {
        createTestDocuments();
        ObjectId toAdd = ObjectId.get();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .push("idSet", toAdd);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertTrue(resultDoc.idSet.contains(toAdd));
    }

    @Test
    public void testPushWithOptions() {
        createTestDocuments();

        ObjectId toAdd = ObjectId.get();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class).filter("embeddedDocs.field1 in",
                List.of("foo1"));
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .push("embeddedDocs.$.objectIds", toAdd, PushOptions.options().position(1));

        UpdateResult result = getDs().update(query, update, new UpdateOptions().multi(true));
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.embeddedDocs.get(0).objectIds.get(1), toAdd);
    }

    @Test
    public void testPushList() {
        createTestDocuments();
        List<ObjectId> toAdd = List.of(ObjectId.get(), ObjectId.get());

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .push("idSet", toAdd);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        for (ObjectId added : toAdd) {
            Assert.assertTrue(resultDoc.idSet.contains(added));
        }
    }

    @Test
    public void testRemoveAll() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        EmbeddedDocument onlyField2 = new EmbeddedDocument(null, "bar1", null);

        UpdateOperations<MyDocument> removeAllOp = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs", onlyField2);

        UpdateResult result = getDs().update(query, removeAllOp);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertFalse(resultDoc.embeddedDocs.contains(testEmbeddedDoc1));
    }

    @Test
    public void testRemoveAllList() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class).filter("embeddedDocs.field1", testEmbeddedDoc2.field1);

        UpdateOperations<MyDocument> removeAllOp = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs.$.objectIds", testObjectIds);

        UpdateResult result = getDs().update(query, removeAllOp);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertTrue(resultDoc.embeddedDocs.get(1).objectIds.isEmpty());
    }

    @Test
    public void testRemoveAllWithFilter() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        Filter filter = Filters.eq("field3", "baz2");

        UpdateOperations<MyDocument> removeAllOp = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs", filter);

        UpdateResult result = getDs().update(query, removeAllOp);
        Assert.assertEquals(result.getModifiedCount(), 1);

        Query<MyDocument> updatedQuery = getDs().find(MyDocument.class);
        MyDocument resultDoc = updatedQuery.first();
        Assert.assertFalse(resultDoc.embeddedDocs.contains(testEmbeddedDoc2));
    }

    @Test
    public void testRemoveScalarValues() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class).filter("embeddedDocs.field1", testEmbeddedDoc1.field1);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs.$.objectIds", testObjectIds.get(0));

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        Query<MyDocument> updatedQuery = getDs().createQuery(MyDocument.class, query.toDocument());
        MyDocument resultDoc = updatedQuery.first();
        resultDoc.embeddedDocs.forEach((em) -> {
            if (em.field1.equals(testEmbeddedDoc1.field1)) {
                Assert.assertFalse(em.objectIds.contains(testObjectIds.get(0)));
            }
        });
    }

    @Test
    public void testRemoveFirst() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .removeFirst("embeddedDocs");

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertFalse(resultDoc.embeddedDocs.contains(testEmbeddedDoc1));
    }

    @Test
    public void testRemoveLast() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .removeLast("embeddedDocs");

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertFalse(resultDoc.embeddedDocs.contains(testEmbeddedDoc2));
    }

    @Test
    public void testSet() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .set("intField", 100);

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, 100);
    }

    @Test
    public void testSetOnInsert() {
        ObjectId id = ObjectId.get();
        Query<MyDocument> query = getDs().createQuery(MyDocument.class).filter("id = ", id);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .setOnInsert("intField", 999);

        UpdateResult result = getDs().update(query, update, new UpdateOptions().upsert(true));
        Assert.assertEquals(result.getModifiedCount(), 0);
        Assert.assertEquals(result.getUpsertedId().asObjectId().getValue(), id);

        MyDocument resultDoc = findMyDocument();
        Assert.assertEquals(resultDoc.intField, 999);
    }

    @Test
    public void testUnset() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .unset("intField");

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);

        MyDocument resultDoc = findMyDocument();
        Assert.assertNull(resultDoc.intField);
    }

    @Test
    public void testUpdateToString() {
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs", testEmbeddedDoc1);

        String test = update.toString();
        Assert.assertTrue(test != null && !test.isEmpty());
    }

    private MyDocument findMyDocument() {
        Query<MyDocument> updatedQuery = getDs().find(MyDocument.class);
        return updatedQuery.first();
    }

    private List<ObjectId> createTestDocuments() {
        MyDocument myDocument = new MyDocument();
        myDocument.intField = 1;
        myDocument.embeddedDocs.addAll(List.of(testEmbeddedDoc1, testEmbeddedDoc2));
        myDocument = getDs().save(myDocument);
        return List.of(myDocument.id);
    }

    @Entity("Documents")
    public static class MyDocument {

        @Id
        public ObjectId id;

        public List<EmbeddedDocument> embeddedDocs = new ArrayList<>();

        public Set<ObjectId> idSet = new HashSet<>();

        public Integer intField;
    }

    @Entity
    public static class EmbeddedDocument {
        public String field1;
        public String field2;
        public String field3;

        public List<ObjectId> objectIds = new ArrayList<>();

        public EmbeddedDocument(String field1, String field2, String field3, List<ObjectId> objectIds) {
            this(field1, field2, field3);
            this.objectIds.addAll(objectIds);
        }

        public EmbeddedDocument(String field1, String field2, String field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EmbeddedDocument that = (EmbeddedDocument) o;
            return Objects.equals(field1, that.field1) && Objects.equals(field2, that.field2)
                    && Objects.equals(field3, that.field3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1, field2, field3);
        }
    }

}
