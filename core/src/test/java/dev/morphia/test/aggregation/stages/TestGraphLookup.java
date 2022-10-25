package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Employee;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.GraphLookup.graphLookup;

public class TestGraphLookup extends AggregationTest {
    @Test
    public void testGraphLookup() {
        List<Document> list = parseDocs("{ '_id' : 1, 'name' : 'Dev' }",
                "{ '_id' : 2, 'name' : 'Eliot', 'reportsTo' : 'Dev' }",
                "{ '_id' : 3, 'name' : 'Ron', 'reportsTo' : 'Eliot' }",
                "{ '_id' : 4, 'name' : 'Andrew', 'reportsTo' : 'Eliot' }",
                "{ '_id' : 5, 'name' : 'Asya', 'reportsTo' : 'Ron' }",
                "{ '_id' : 6, 'name' : 'Dan', 'reportsTo' : 'Andrew' }");

        insert("employees", list);

        List<Document> actual = getDs().aggregate(Employee.class)
                .graphLookup(graphLookup("employees")
                        .startWith(field("reportsTo"))
                        .connectFromField("reportsTo")
                        .connectToField("name")
                        .as("reportingHierarchy"))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs("{'_id': 1, 'name': 'Dev', 'reportingHierarchy': []}",
                "{'_id': 2, 'name': 'Eliot', 'reportsTo': 'Dev', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'}]}",
                "{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                        + "'Eliot', 'reportsTo': 'Dev'}]}",
                "{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                        + "'Eliot', 'reportsTo': 'Dev'}]}",
                "{'_id': 5, 'name': 'Asya', 'reportsTo': 'Ron', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                        + "'Eliot', 'reportsTo': 'Dev'},{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot'}]}",
                "{'_id': 6, 'name': 'Dan', 'reportsTo': 'Andrew', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                        + "'Eliot', 'reportsTo': 'Dev'},{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot'}]}");

        assertDocumentEquals(actual, expected);
    }

}
