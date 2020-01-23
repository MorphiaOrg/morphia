package dev.morphia.mapping.codec;

import dev.morphia.TestBase;
import org.bson.Document;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

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
        String s = writer.<Document>getRoot().toJson();
        JSONAssert.assertEquals(expected, s, false);
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
    }

    private void check(final DocumentWriter writer, final int docs, final int arrays) {
        assertEquals(docs, writer.getDocsLevel());
        assertEquals(arrays, writer.getArraysLevel());
    }

}
