package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateTrunc;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.TimeUnit.WEEK;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.time.DayOfWeek.MONDAY;

public class TestDateTrunc extends AggregationTest {
    @Test
    public void testDateTrunc() {
        checkMinServerVersion("5.0.0");

        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                .project(project()
                        .include("orderDate")
                        .include("truncatedOrderDate", dateTrunc(field("orderDate"), WEEK)
                                .binSize(2)
                                .timezone(value("America/Los_Angeles"))
                                .startOfWeek(MONDAY)))
                .execute(Document.class)
                .toList();

        List<Document> expected = parseDocs(
                "{ _id: 0, orderDate: ISODate('2020-05-18T14:10:30.000Z'), truncatedOrderDate: ISODate('2020-05-11T07:00:00.000Z') }",
                "{ _id: 1, orderDate: ISODate('2021-03-20T11:30:05.000Z'), truncatedOrderDate: ISODate('2021-03-15T07:00:00.000Z') }",
                "{ _id: 2, orderDate: ISODate('2021-01-11T06:31:15.000Z'), truncatedOrderDate: ISODate('2021-01-04T08:00:00.000Z') }",
                "{ _id: 3, orderDate: ISODate('2020-02-08T13:13:23.000Z'), truncatedOrderDate: ISODate('2020-02-03T08:00:00.000Z') }",
                "{ _id: 4, orderDate: ISODate('2019-05-18T16:09:01.000Z'), truncatedOrderDate: ISODate('2019-05-13T07:00:00.000Z') }",
                "{ _id: 5, orderDate: ISODate('2019-01-08T06:12:03.000Z'), truncatedOrderDate: ISODate('2019-01-07T08:00:00.000Z') }");

        assertListEquals(actual, expected);
    }

}
