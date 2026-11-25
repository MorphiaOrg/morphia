package dev.morphia.test.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.AggregationOptions.aggregationOptions;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static org.bson.Document.parse;

/**
 * The {@code $count} accumulator's examples, with their data inline until extraction writes their scenarios. They are
 * the only tests of {@link AccumulatorExpressions#count()}.
 */
public class TestCount extends TestBase {
    private static final List<String> CAKE_SALES = List.of(
            "{ _id: 0, type: 'chocolate', orderDate: ISODate('2020-05-18T14:10:30Z'), state: 'CA', price: 13, quantity: 120 }",
            "{ _id: 1, type: 'chocolate', orderDate: ISODate('2021-03-20T11:30:05Z'), state: 'WA', price: 14, quantity: 140 }",
            "{ _id: 2, type: 'vanilla', orderDate: ISODate('2021-01-11T06:31:15Z'), state: 'CA', price: 12, quantity: 145 }",
            "{ _id: 3, type: 'vanilla', orderDate: ISODate('2020-02-08T13:13:23Z'), state: 'WA', price: 13, quantity: 104 }",
            "{ _id: 4, type: 'strawberry', orderDate: ISODate('2019-05-18T16:09:01Z'), state: 'CA', price: 41, quantity: 162 }",
            "{ _id: 5, type: 'strawberry', orderDate: ISODate('2019-01-08T06:12:03Z'), state: 'WA', price: 43, quantity: 134 }");

    @BeforeEach
    public void seed() {
        checkMinServerVersion("5.0.0");
        getDatabase().getCollection("cakeSales").insertMany(CAKE_SALES.stream().map(Document::parse).toList());
    }

    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        List<Document> actual = getDs().aggregate(aggregationOptions().collection("cakeSales"))
                .pipeline(group(Group.id("$state")).field("countNumberOfDocumentsForState", AccumulatorExpressions.count()))
                .toList();

        assertDocumentListEquals(List.of(
                parse("{ '_id' : 'CA', 'countNumberOfDocumentsForState' : 3 }"),
                parse("{ '_id' : 'WA', 'countNumberOfDocumentsForState' : 3 }")), actual, false);
    }

    @Test
    @DisplayName("Use in ``$setWindowFields`` Stage")
    public void testUseInSetWindowFieldsStage() {
        List<Document> actual = getDs().aggregate(aggregationOptions().collection("cakeSales"))
                .pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("orderDate"))
                        .output(output("countNumberOfDocumentsForState").operator(AccumulatorExpressions.count()).window()
                                .documents("unbounded", "current")))
                .toList();

        // Each state's sales, counted up to and including each sale in date order.
        assertDocumentListEquals(List.of(
                withCount(CAKE_SALES.get(4), 1), withCount(CAKE_SALES.get(0), 2), withCount(CAKE_SALES.get(2), 3),
                withCount(CAKE_SALES.get(5), 1), withCount(CAKE_SALES.get(3), 2), withCount(CAKE_SALES.get(1), 3)),
                actual, false);
    }

    private static Document withCount(String sale, int count) {
        return parse(sale).append("countNumberOfDocumentsForState", count);
    }
}
