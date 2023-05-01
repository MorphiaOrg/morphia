package dev.morphia.test.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.aggregation.stages.Merge;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateAdd extends AggregationTest {
    @Test
    public void testDateAdd() {
        checkMinServerVersion(5.0);
        checkMinDriverVersion(4.2);

        insert("shipping", parseDocs(
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b2'), custId: 456, purchaseDate: ISODate('2020-12-31') }",
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b3'), custId: 457, purchaseDate: ISODate('2021-02-28') }",
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b4'), custId: 458, purchaseDate: ISODate('2021-02-26') }"));

        getDs().aggregate("shipping")
                .project(project()
                        .include("expectedDeliveryDate", dateAdd(field("purchaseDate"), 3, DAY)))
                .merge(Merge.into("shipping"));

        List<Document> actual = getDatabase().getCollection("shipping").find().into(new ArrayList<>());
        List<Document> expected = parseDocs(
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b2'), 'custId' : 456, 'purchaseDate' : ISODate('2020-12-31T00:00:00Z'), " +
                        "'expectedDeliveryDate' : ISODate('2021-01-03T00:00:00Z') }",
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b3'), 'custId' : 457, 'purchaseDate' : ISODate('2021-02-28T00:00:00Z'), " +
                        "'expectedDeliveryDate' : ISODate('2021-03-03T00:00:00Z') }",
                "{ '_id' : ObjectId('603dd4b2044b995ad331c0b4'), 'custId' : 458, 'purchaseDate' : ISODate('2021-02-26T00:00:00Z'), " +
                        "'expectedDeliveryDate' : ISODate('2021-03-01T00:00:00Z') }");

        assertListEquals(actual, expected);
    }

}
