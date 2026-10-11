package dev.morphia.test.mapping.codec;

import java.util.Map;

import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.query.filters.Filters.*;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.bson.Document.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("CodeBlock2Expr")
public class TestDocumentWriter extends TestBase {

    @Test
    public void testAnd() {
        MorphiaQuery<User> query = (MorphiaQuery<User>) getDs().find(User.class).disableValidation();
        query.filter(Filters.gte("field1", 100));
        query.filter(Filters.lt("field1", 1000));

        query.filter(Filters.gte("field2", 200));
        query.filter(Filters.lt("field2", 2000));

        Document document = query.toDocument();
        Assertions.assertEquals(2, ((Map<?, ?>) document.get("field1")).size());
        Assertions.assertEquals(2, ((Map<?, ?>) document.get("field2")).size());
    }

    @Test
    public void testOr() {
        MorphiaQuery query = (MorphiaQuery) getDs().find(User.class)
                .disableValidation();

        query.filter(or(eq("name", "A"), eq("name", "B")));
        query.filter(or(eq("name", "C"), eq("name", "D")));
        Document document = query.toDocument();
        document.remove("_t");
        Document expected = new Document("$and",
                of(
                        new Document("$or", of(new Document("name", "A"), new Document("name", "B"))),
                        new Document("$or", of(new Document("name", "C"), new Document("name", "D")))));
        assertDocumentEquals(document, expected);
    }

    @Test
    public void seededQuery() {
        Document seed = new Document("field1", new Document("$gt", 1))
                .append("$or", of(new Document("name", "A"), new Document("name", "B")));
        Document original = new Document(seed);
        MorphiaQuery<User> query = (MorphiaQuery<User>) getDs().find(User.class, seed).disableValidation();
        query.filter(lt("field1", 5));
        query.filter(or(eq("name", "C"), eq("name", "D")));

        Document expected = new Document("field1", new Document("$gt", 1).append("$lt", 5))
                .append("$and", of(
                        new Document("$or", of(new Document("name", "A"), new Document("name", "B"))),
                        new Document("$or", of(new Document("name", "C"), new Document("name", "D")))));
        for (int i = 0; i < 2; i++) {
            Document document = query.toDocument();
            document.remove("_t");
            assertEquals(expected, document);
        }
        assertEquals(original, seed);
    }

    @Test
    public void deepMerge() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
        document(writer, () -> {
            document(writer, "a", () -> {
                document(writer, "b", () -> writer.writeInt32("x", 1));
            });
            document(writer, "a", () -> {
                document(writer, "b", () -> writer.writeInt32("y", 2));
            });
        });

        assertEquals(new Document("a", new Document("b", new Document("x", 1).append("y", 2))), writer.getDocument());
    }

    @Test
    public void plainDocumentEquality() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
        document(writer, () -> {
            document(writer, "subdoc", () -> writer.writeInt32("nested", 42));
        });

        Document expected = new Document("subdoc", new Document("nested", 42));
        assertEquals(expected, writer.getDocument());
        assertEquals(writer.getDocument(), expected);
    }

    @Test
    public void arrays() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());

        document(writer, () -> {
            array(writer, "stuff", () -> {
                writer.writeString("hello");
                writer.writeInt32(42);
            });
            writer.writeName("next");
            writer.writeString("something simple");
        });

        assertDocumentEquals(new Document("stuff", asList("hello", 42)).append("next", "something simple"), writer.getDocument());
    }

    @Test
    public void arraysWithDocs() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());

        document(writer, () -> {
            array(writer, "stuff", () -> {
                document(writer, () -> {
                    writer.writeInt32("doc", 42);
                });
            });
        });

        assertDocumentEquals(new Document("stuff", of(new Document("doc", 42))), writer.getDocument());
    }

    @Test
    public void basic() {
        for (int i = 0; i < 3; i++) {
            DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
            Document expected = new Document();

            int finalI = i;
            document(writer, () -> {
                for (int j = 0; j < finalI; j++) {
                    writer.writeInt32("entry " + j, j);
                    expected.put("entry " + j, j);
                }
            });
            assertDocumentEquals(expected, writer.getDocument());
        }
    }

    @Test
    public void nestedArrays() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());

        document(writer, () -> {
            array(writer, "top", () -> {
                array(writer, () -> {
                    writer.writeInt32(1);
                    writer.writeInt32(2);
                    writer.writeInt32(3);
                    document(writer, () -> writer.writeString("nested", "string"));
                });
            });
        });
        Document top = new Document("top", of(of(1, 2, 3, new Document("nested", "string"))));
        assertDocumentEquals(writer.getDocument(), top);
    }

    @Test
    public void nesting() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
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
        assertDocumentEquals(parse("{$group : {_id : {$dateToString: {format: \"%Y-%m-%d\", date: \"$date\"}}, totalSaleAmount: {$sum: "
                + "{$multiply: [ \"$price\", \"$quantity\" ]}}, averageQuantity: {$avg: \"$quantity\"},count: {$sum: 1}}}"),
                writer.getDocument(), false);
    }

    @Test
    public void subdocuments() {
        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
        document(writer, () -> {
            writer.writeName("subdoc");
            document(writer, () -> writer.writeInt32("nested", 42));
        });

        assertDocumentEquals(new Document("subdoc", new Document("nested", 42)), writer.getDocument());

    }
}
