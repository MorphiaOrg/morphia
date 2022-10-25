package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Sales;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;

public class TestAdd extends AggregationTest {
    @Test
    public void testAdd() {
        getMapper().map(Sales.class);
        insert("sales", asList(
                parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'fee' : 2, date: ISODate('2014-03-01T08:00:00Z') }"),
                parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'fee' : 1, date: ISODate('2014-03-01T09:00:00Z') }"),
                parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5,  'fee' : 0, date: ISODate('2014-03-15T09:00:00Z') }")));
        Aggregation<Sales> pipeline = getDs()
                .aggregate(Sales.class)
                .project(project()
                        .include("item")
                        .include("total",
                                add(field("price"), field("fee"))));

        List<Document> list = pipeline.execute(Document.class).toList();
        List<Document> expected = asList(
                parse("{ '_id' : 1, 'item' : 'abc', 'total' : 12 }"),
                parse("{ '_id' : 2, 'item' : 'jkl', 'total' : 21 }"),
                parse("{ '_id' : 3, 'item' : 'xyz', 'total' : 5 } }"));
        for (int i = 1; i < 4; i++) {
            compare(i, expected, list);
        }
    }
}
