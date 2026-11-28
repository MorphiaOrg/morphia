package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.gt;

public class TestCond extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/cond/condExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testCondExpressionOperator() {
        testPipeline(aggregation -> aggregation
                .pipeline(project().include("item").include("discount", condition(gte("$qty", 250), 30, 20))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/cond/priceRentalsByRating
     */
    @Test
    @DisplayName("Price Rentals by Rating")
    public void testPriceRentalsByRating() {
        testPipeline(aggregation -> aggregation.pipeline(match(gt("runtime", 1000)),
                project().include("title").include("rentalPrice", condition(gte("$imdb.rating", 9), 5.99, 3.99))));
    }
}
