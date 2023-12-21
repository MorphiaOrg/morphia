package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.query.Type;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.Author;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceRoot.replaceRoot;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.type;
import static java.util.stream.Collectors.toList;

public class TestReplaceRoot extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceRoot(mergeObjects()
                        .add(document()
                                .field("dogs", value(0))
                                .field("cats", value(0))
                                .field("birds", value(0))
                                .field("fish", value(0)))
                        .add(field("pets")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                unwind("grades"),
                match(gte("grades.grade", 90)),
                replaceRoot(field("grades"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceRoot()
                        .field("full_name", concat(field("first_name"), value(" "), field("last_name")))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                replaceRoot(mergeObjects()
                        .add(document()
                                .field("_id", value(""))
                                .field("name", value(""))
                                .field("email", value(""))
                                .field("cell", value(""))
                                .field("home", value("")))
                        .add(ROOT))));
    }

    @Test
    public void testReplaceRoot() {
        List<Document> documents = parseDocs(
                "{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}",
                "{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}",
                "{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}",
                "{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}");

        insert("authors", documents);

        List<Document> actual = getDs().aggregate(Author.class)
                .match(exists("name"),
                        type("name", Type.ARRAY).not(),
                        type("name", Type.OBJECT))
                .replaceRoot(replaceRoot(field("name")))
                .execute(Document.class)
                .toList();
        List<Document> expected = documents.subList(0, 3)
                .stream()
                .map(d -> (Document) d.get("name"))
                .collect(toList());
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                .replaceRoot(replaceRoot(ifNull().target(field("name"))
                        .field("_id", field("_id"))
                        .field("missingName", value(true))))
                .execute(Document.class)
                .toList();
        expected = documents.subList(0, 3)
                .stream()
                .map(d -> (Document) d.get("name"))
                .collect(toList());
        expected.add(new Document("_id", 4)
                .append("missingName", true));
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                .replaceRoot(replaceRoot(mergeObjects()
                        .add(document()
                                .field("_id", field("_id"))
                                .field("first", value(""))
                                .field("last", value("")))
                        .add(field("name"))))
                .execute(Document.class)
                .toList();
        expected = documents.subList(0, 3)
                .stream()
                .peek(d -> d.putAll((Document) d.remove("name")))
                .collect(toList());
        expected.add(new Document("_id", 4)
                .append("first", "")
                .append("last", ""));
        assertDocumentEquals(actual, expected);
    }

}
