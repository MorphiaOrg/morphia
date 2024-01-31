package dev.morphia.test;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.mongodb.client.result.UpdateResult;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;

@SuppressWarnings("removal")
public class TestLegacyUpdate extends TestBase {

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
  }

  private void createTestDocuments() {
    MyDocument myDocument = new MyDocument();
    EmbeddedDocument em1 = new EmbeddedDocument(
        "foo1",
        "bar1",
        "baz1");

    EmbeddedDocument em2 = new EmbeddedDocument(
        "foo2",
        "bar2",
        "baz2");

    myDocument.embeddedDocs.addAll(List.of(em1, em2));
    getDs().save(myDocument);
  }

  @Entity("Documents")
  public static class MyDocument {

    @Id
    public ObjectId id;

    public List<EmbeddedDocument> embeddedDocs = new ArrayList<>();
  }

  @Entity
  public static class EmbeddedDocument {
    public String field1;
    public String field2;
    public String field3;

    public EmbeddedDocument(String field1, String field2, String field3) {
      this.field1 = field1;
      this.field2 = field2;
      this.field3 = field3;
    }
  }

}
