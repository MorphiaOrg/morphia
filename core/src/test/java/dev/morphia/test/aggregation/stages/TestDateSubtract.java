package dev.morphia.test.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateSubtract;
import static dev.morphia.aggregation.expressions.DateExpressions.month;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;

public class TestDateSubtract extends AggregationTest {
    @Test
    public void testDateSubtract() {
        checkMinServerVersion("5.0.0");

        insert("connectionTime", parseDocs(
                "{ _id: 1, custId: 457, login: ISODate('2020-12-25T19:04:00Z'), logout: ISODate('2020-12-28T09:04:00Z')}",
                "{ _id: 2, custId: 457, login: ISODate('2021-01-27T05:12:00Z'), logout: ISODate('2021-01-28T13:05:00Z') }",
                "{ _id: 3, custId: 458, login: ISODate('2021-01-22T06:27:00Z'), logout: ISODate('2021-01-31T11:00:00Z') }",
                "{ _id: 4, custId: 459, login: ISODate('2021-02-14T20:14:00Z'), logout: ISODate('2021-02-17T16:05:00Z') }",
                "{ _id: 5, custId: 460, login: ISODate('2021-02-26T02:44:00Z'), logout: ISODate('2021-02-18T14:13:00Z') }"));

        getDs().aggregate("connectionTime")
                .match(
                        expr(ComparisonExpressions.eq(year(field("logout")), literal(2021))),
                        expr(ComparisonExpressions.eq(month(field("logout")), literal(1))))
                .project(project()
                        .include("logoutTime", dateSubtract(field("logout"), 3, HOUR)))
                .merge(Merge.into("connectionTime"));

        List<Document> actual = getDatabase().getCollection("connectionTime").find().into(new ArrayList<>());
        List<Document> expected = parseDocs(
                "{ '_id' : 1, 'custId' : 457, 'login' : ISODate('2020-12-25T19:04:00Z'), 'logout' : ISODate('2020-12-28T09:04:00Z')}",
                "{ '_id' : 2, 'custId' : 457, 'login' : ISODate('2021-01-27T05:12:00Z'), 'logout' : ISODate('2021-01-28T13:05:00Z'), " +
                        "'logoutTime' : ISODate('2021-01-28T10:05:00Z') }",
                "{ '_id' : 3, 'custId' : 458, 'login' : ISODate('2021-01-22T06:27:00Z'), 'logout' : ISODate('2021-01-31T11:00:00Z'), " +
                        "'logoutTime' : ISODate('2021-01-31T08:00:00Z') }",
                "{ '_id' : 4, 'custId' : 459, 'login' : ISODate('2021-02-14T20:14:00Z'), 'logout' : ISODate('2021-02-17T16:05:00Z') }",
                "{ '_id' : 5, 'custId' : 460, 'login' : ISODate('2021-02-26T02:44:00Z'), 'logout' : ISODate('2021-02-18T14:13:00Z') }");

        assertListEquals(actual, expected);
    }

}
