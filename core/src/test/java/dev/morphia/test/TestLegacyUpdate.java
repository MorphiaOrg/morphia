package dev.morphia.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mongodb.client.result.UpdateResult;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
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
    public void testRemoveAllUpdate() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        EmbeddedDocument onlyField2 = new EmbeddedDocument(null, "bar1", null);

        UpdateOperations<MyDocument> removeAllOp = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs", onlyField2);

        UpdateResult result = getDs().update(query, removeAllOp);
        Assert.assertEquals(result.getModifiedCount(), 1);

        Query<MyDocument> updatedQuery = getDs().find(MyDocument.class);
        MyDocument resultDoc = updatedQuery.first();
        Assert.assertFalse(resultDoc.embeddedDocs.contains(testEmbeddedDoc1));
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
    public void testUpdateToString() {
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .removeAll("embeddedDocs", testEmbeddedDoc1);

        String test = update.toString();
        Assert.assertTrue(test != null && !test.isEmpty());
    }

    @Test
    public void testAddToSet() {
        createTestDocuments();

        Query<MyDocument> query = getDs().createQuery(MyDocument.class);
        UpdateOperations<MyDocument> update = getDs().createUpdateOperations(MyDocument.class)
                .addToSet("idSet", ObjectId.get());

        UpdateResult result = getDs().update(query, update);
        Assert.assertEquals(result.getModifiedCount(), 1);
    }

    private List<ObjectId> createTestDocuments() {
        MyDocument myDocument = new MyDocument();
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
