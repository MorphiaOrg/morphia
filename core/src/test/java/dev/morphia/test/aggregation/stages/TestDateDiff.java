package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.DateExpressions.dateDiff;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Projection.project;
import static org.testng.Assert.assertEquals;

public class TestDateDiff extends AggregationTest {
    @Test
    public void testDateDiff() {
        checkMinServerVersion(5.0);

        insert("orders", parseDocs(
                "{ _id: 1, custId: 456, purchased: ISODate('2020-12-31'), delivered: ISODate('2021-01-05') }",
                "{ _id: 2, custId: 457, purchased: ISODate('2021-02-28'), delivered: ISODate('2021-03-07') }",
                "{ _id: 3, custId: 458, purchased: ISODate('2021-02-16'), delivered: ISODate('2021-02-18') }"));

        Document actual = getDs().aggregate("orders")
                .group(group()
                        .field("averageTime", avg(dateDiff(field("purchased"), field("delivered"), DAY))))
                .project(project()
                        .suppressId()
                        .include("numDays", trunc(field("averageTime"), literal(1))))
                .execute(Document.class)
                .next();

        Document expected = new Document("numDays", 4.6D);

        assertEquals(actual, expected);
    }

}
