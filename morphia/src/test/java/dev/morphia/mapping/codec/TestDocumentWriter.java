package dev.morphia.mapping.codec;

import dev.morphia.TestBase;
import org.bson.Document;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TestDocumentWriter extends TestBase {
    @Test
    public void nesting() throws JSONException {
        String expected = "{$group : {_id : {$dateToString: {format: \"%Y-%m-%d\", date: \"$date\"}}, totalSaleAmount: {$sum: "
                          + "{$multiply: [ \"$price\", \"$quantity\" ]}}, averageQuantity: {$avg: \"$quantity\"},count: {$sum: 1}}}";

        DocumentWriter writer = new DocumentWriter();
        writer.writeStartDocument();
        writer.writeStartDocument("$group");

        writer.writeStartDocument("_id");
        writer.writeStartDocument("$dateToString");
        writer.writeString("format", "%Y-%m-%d");
        writer.writeString("date", "$date");
        writer.writeEndDocument();
        writer.writeEndDocument();

        writer.writeStartDocument("totalSaleAmount");
        writer.writeStartDocument("$sum");
        writer.writeStartArray("$multiply");
        writer.writeString("$price");
        writer.writeString("$quantity");
        writer.writeEndArray();
        writer.writeEndDocument();
        writer.writeEndDocument();

        writer.writeStartDocument("averageQuantity");
        writer.writeString("$avg", "$quantity");
        writer.writeEndDocument();

        writer.writeStartDocument("count");
        writer.writeInt32("$sum", 1);
        writer.writeEndDocument();

        writer.writeEndDocument();
        writer.writeEndDocument();
        String s = writer.getDocument().toJson();
        JSONAssert.assertEquals(expected, s, false);
    }

    @Test
    public void testBasic() {
        DocumentWriter writer = new DocumentWriter();

        writer.writeStartDocument();
        writer.writeEndDocument();

        check(writer, 0, 0);
        Document document = writer.getDocument();
        assertEquals(new Document(), document);

        writer = new DocumentWriter();
        writer.writeStartDocument();
        writer.writeName("first");
        writer.writeInt32(42);
        writer.writeName("second");
        writer.writeBoolean(false);
        writer.writeInt64("third", 100L);
        writer.writeEndDocument();

        check(writer, 0, 0);
        assertEquals(new Document("first", 42)
                         .append("second", false)
                         .append("third", 100L),
            writer.getDocument());
    }

    @Test
    public void testDuplicateKeys() {
        DocumentWriter writer = new DocumentWriter();
        writer.writeStartDocument();

        writer.writeStartDocument("id");
        writer.writeInt32("first", 1);
        writer.writeEndDocument();

        writer.writeStartDocument("id");
        writer.writeInt32("second", 2);
        writer.writeEndDocument();

        writer.writeEndDocument();

        Document document = (Document) writer.getDocument().get("id");

        Assert.assertTrue(document.toString(), document.containsKey("first"));
        Assert.assertTrue(document.toString(), document.containsKey("second"));
    }

    @Test
    public void testArrays() {
        DocumentWriter writer = new DocumentWriter();

        writer.writeStartDocument();
        check(writer, 1, 0);
        writer.writeStartArray("stuff");
        check(writer, 1, 1);
        writer.writeString("hello");
        writer.writeInt32(42);
        writer.writeEndArray();
        check(writer, 1, 0);
        writer.writeName("next");
        writer.writeString("something simple");
        writer.writeEndDocument();

        check(writer, 0, 0);
        assertEquals(new Document("stuff", asList("hello", 42))
                         .append("next", "something simple"), writer.getDocument());
    }

    @Test
    public void testArraysWithDocs() {
        DocumentWriter writer = new DocumentWriter();

        writer.writeStartDocument();
        check(writer, 1, 0);
        writer.writeStartArray("stuff");
        check(writer, 1, 1);
        writer.writeStartDocument();
        writer.writeInt32("doc", 42);
        writer.writeEndDocument();
        writer.writeEndArray();
        check(writer, 1, 0);
        writer.writeEndDocument();

        check(writer, 0, 0);
        assertEquals(new Document("stuff", asList(new Document("doc", 42))), writer.getDocument());
    }

    @Test
    public void testSubdocuments() {
        DocumentWriter writer = new DocumentWriter();
        writer.writeStartDocument();
        writer.writeName("subdoc");
        writer.writeStartDocument();
        writer.writeInt32("nested", 42);
        writer.writeEndDocument();
        writer.writeEndDocument();

        check(writer, 0, 0);
        assertEquals(new Document("subdoc", new Document("nested", 42)), writer.getDocument());

    }


    private void check(final DocumentWriter writer, final int docs, final int arrays) {
        assertEquals(docs, writer.getDocsLevel());
        assertEquals(arrays, writer.getArraysLevel());
    }

}
