package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.aggregation.AggregationTest;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.query.Sort.ascending;

public class TestFill extends AggregationTest {
    @Override
    public String prefix() {
        return "fill";
    }

    @Test
    public void testConstantValue() {
        testCase(5.3, "constantValue", "dailySales", (collection) -> {
            return getDs().aggregate(collection)
                          .fill(fill()
                                    .field("bootsSold", value(0))
                                    .field("sandalsSold", value(0))
                                    .field("sneakersSold", value(0)));
        });
    }

    @Test
    public void testDistinctPartitions() {
        testCase(5.3, "distinctPartitions", "restaurantReviewsMultiple", (collection) -> {
            return getDs().aggregate(collection)
                          .fill(fill()
                                    .sortBy(ascending("date"))
                                    .partitionBy(document("restaurant", field("restaurant")))
                                    .field("score", Method.LOCF)
                               );
        });
    }

    @Test
    public void testLastObserved() {
        testCase(5.3, "lastObserved", "restaurantReviews", (collection) -> {
            return getDs().aggregate(collection)
                          .fill(fill()
                                    .sortBy(ascending("date"))
                                    .field("score", Method.LOCF));
        });
    }

    @Test
    public void testLinearInterpolation() {
        testCase(5.3, "linearInterpolation", "stock", (collection) -> {
            return getDs().aggregate(collection)
                          .fill(fill()
                                    .sortBy(ascending("time"))
                                    .field("price", Method.LINEAR));
        });
    }
}

