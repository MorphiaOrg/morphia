package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;

public class TestSetWindowFields extends AggregationTest {
    @Test
    public void testSetWindowFields() {
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(ascending("orderDate"))
                        .output(output("cumulativeQuantityForState")
                                .operator(sum(field("quantity")))
                                .window()
                                .documents("unbounded", "current")))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, " +
                        "'quantity' : 162, 'cumulativeQuantityForState' : 162 }",
                "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, " +
                        "'quantity' : 120, 'cumulativeQuantityForState' : 282 }",
                "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, " +
                        "'quantity' : 145, 'cumulativeQuantityForState' : 427 }",
                "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, " +
                        "'quantity' : 134, 'cumulativeQuantityForState' : 134 }",
                "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, " +
                        "'quantity' : 104, 'cumulativeQuantityForState' : 238 }",
                "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, " +
                        "'quantity' : 140, 'cumulativeQuantityForState' : 378 }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testShift() {
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState")
                                .operator(shift(field("quantity"), 1, value("Not available")))))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, 'quantity' "
                        +
                        ": 162, 'shiftQuantityForState' : 145 }",
                "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, 'quantity' : "
                        +
                        "145, 'shiftQuantityForState' : 120 }",
                "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, 'quantity' :"
                        +
                        " 120, 'shiftQuantityForState' : 'Not available' }",
                "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, 'quantity' :"
                        +
                        " 140, 'shiftQuantityForState' : 134 }",
                "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, 'quantity' "
                        +
                        ": 134, 'shiftQuantityForState' : 104 }",
                "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, 'quantity' : "
                        +
                        "104, 'shiftQuantityForState' : 'Not available' }");

        assertListEquals(actual, expected);
    }
}
