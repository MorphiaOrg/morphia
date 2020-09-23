package dev.morphia.mapping.codec;

import dev.morphia.TestBase;
import org.bson.Document;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TestDocumentWriter extends TestBase {
    private int docs = 0;
    private int arrays = 0;

    @Test
    public void nesting() throws JSONException {
        String expected = "{$group : {_id : {$dateToString: {format: \"%Y-%m-%d\", date: \"$date\"}}, totalSaleAmount: {$sum: "
                          + "{$multiply: [ \"$price\", \"$quantity\" ]}}, averageQuantity: {$avg: \"$quantity\"},count: {$sum: 1}}}";

        DocumentWriter writer = new DocumentWriter();
        document(writer, () -> {
            document(writer, "$group", () -> {

                document(writer, "_id", () -> {
                    document(writer, "$dateToString", () -> {
                        writer.writeString("format", "%Y-%m-%d");
                        writer.writeString("date", "$date");
                    });
                });

                document(writer, "totalSaleAmount", () -> {
                    document(writer, "$sum", () -> {
                        array(writer, "$multiply", () -> {
                            writer.writeString("$price");
                            writer.writeString("$quantity");
                        });
                    });
                });

                document(writer, "averageQuantity", () -> writer.writeString("$avg", "$quantity"));

                document(writer, "count", () -> {
                    writer.writeInt32("$sum", 1);
                });

            });
        });
        String s = writer.getDocument().toJson();
        JSONAssert.assertEquals(expected, s, false);
    }

    @Test
    public void testArrays() {
        DocumentWriter writer = new DocumentWriter();

        document(writer, () -> {
            check(writer, 1, 0);
            array(writer, "stuff", () -> {
                check(writer, 1, 1);
                writer.writeString("hello");
                writer.writeInt32(42);
            });
            check(writer, 1, 0);
            writer.writeName("next");
            writer.writeString("something simple");
        });

        check(writer, 0, 0);
        assertEquals(new Document("stuff", asList("hello", 42))
                         .append("next", "something simple"), writer.getDocument());
    }

    @Test
    public void testArraysWithDocs() {
        DocumentWriter writer = new DocumentWriter();

        document(writer, () -> {
            check(writer, 1, 0);
            array(writer, "stuff", () -> {
                check(writer, 1, 1);
                document(writer, () -> {
                    writer.writeInt32("doc", 42);
                });
            });
            check(writer, 1, 0);
        });

        check(writer, 0, 0);
        assertEquals(new Document("stuff", asList(new Document("doc", 42))), writer.getDocument());
    }

    @Test
    public void testBasic() {
        DocumentWriter writer = new DocumentWriter();

        document(writer, () -> {
        });

        check(writer, 0, 0);
        Document document = writer.getDocument();
        assertEquals(new Document(), document);

        DocumentWriter writer2 = new DocumentWriter();
        document(writer2, () -> {
            writer2.writeName("first");
            writer2.writeInt32(42);
            writer2.writeName("second");
            writer2.writeBoolean(false);
            writer2.writeInt64("third", 100L);
        });

        check(writer2, 0, 0);
        assertEquals(new Document("first", 42)
                         .append("second", false)
                         .append("third", 100L),
            writer2.getDocument());
    }

    @Test
    public void testDuplicateKeys() {
        DocumentWriter writer = new DocumentWriter();
        document(writer, () -> {
            document(writer, "id", () -> writer.writeInt32("first", 1));
            document(writer, "id", () -> writer.writeInt32("second", 2));
        });

        Document document = (Document) writer.getDocument().get("id");

        Assert.assertTrue(document.toString(), document.containsKey("first"));
        Assert.assertTrue(document.toString(), document.containsKey("second"));
    }

    @Test
    public void testNestedArrays() {
        DocumentWriter writer = new DocumentWriter();

        startDoc(writer);
        startArray(writer, "top");
        startArray(writer);
        writer.writeInt32(1);
        writer.writeInt32(2);
        writer.writeInt32(3);
        startDoc(writer);
        writer.writeString("nested", "string");
        endDoc(writer);
        endArray(writer);
        endArray(writer);
        endDoc(writer);
        Document top = new Document("top", List.of(List.of(1, 2, 3, new Document("nested", "string"))));
        assertEquals(top, writer.getDocument());
    }

    @Test
    public void testSubdocuments() {
        DocumentWriter writer = new DocumentWriter();
        document(writer, () -> {
            writer.writeName("subdoc");
            document(writer, () -> writer.writeInt32("nested", 42));
        });

        check(writer, 0, 0);
        assertEquals(new Document("subdoc", new Document("nested", 42)), writer.getDocument());

    }

    private void check(DocumentWriter writer, int docs, int arrays) {
        assertEquals(docs, writer.getDocsLevel());
        assertEquals(arrays, writer.getArraysLevel());
    }

    private void endArray(DocumentWriter writer) {
        writer.writeEndArray();
        assertEquals(--arrays, writer.getArraysLevel());
    }

    private void endDoc(DocumentWriter writer) {
        writer.writeEndDocument();
        assertEquals(--docs, writer.getDocsLevel());
    }

    private void startArray(DocumentWriter writer) {
        writer.writeStartArray();
        assertEquals(++arrays, writer.getArraysLevel());
    }

    private void startArray(DocumentWriter writer, String name) {
        writer.writeStartArray(name);
        assertEquals(++arrays, writer.getArraysLevel());
    }

    private void startDoc(DocumentWriter writer) {
        writer.writeStartDocument();
        assertEquals(++docs, writer.getDocsLevel());
    }

}
