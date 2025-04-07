package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.query.Sort;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.WindowExpressions.covariancePop;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static org.bson.Document.parse;

public class TestCovariancePop extends AggregationTest {
    @Test
    public void testCovariancePop() {
        checkMinServerVersion("5.0.0");
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                .setWindowFields(setWindowFields()
                        .partitionBy(field("state"))
                        .sortBy(Sort.ascending("orderDate"))
                        .output(output("covariancePopForState")
                                .operator(
                                        covariancePop(year(field("orderDate")), field("quantity")))
                                .window()
                                .documents("unbounded", "current")))
                .execute(Document.class)
                .toList();

        List<Document> expected = List.of(
                parse("{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, " +
                        "'quantity' : 162, 'covariancePopForState' : 0.0 }"),
                parse("{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, " +
                        "'quantity' : 120, 'covariancePopForState' : -10.5 }"),
                parse("{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, " +
                        "'quantity' : 145, 'covariancePopForState' : -5.666666666666671 }"),
                parse("{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, " +
                        "'quantity' : 134, 'covariancePopForState' : 0.0 }"),
                parse("{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, " +
                        "'quantity' : 104, 'covariancePopForState' : -7.5 }"),
                parse("{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, " +
                        "'quantity' : 140, 'covariancePopForState' : 2.0 }"));

        assertListEquals(actual, expected);
    }

}
