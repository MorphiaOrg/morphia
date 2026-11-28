package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.query.filters.Filters.expr;

public class TestExpr extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/expr/compareTwoFieldsFromASingleDocument
     */
    @Test
    @DisplayName("Compare Two Fields from a Single Document")
    public void testCompareTwoFieldsFromASingleDocument() {
        testQuery((query) -> query.filter(expr(gt("$tomatoes.viewer.rating", "$tomatoes.critic.rating"))),
                new FindOptions().projection().exclude("_id").projection()
                        .include("title", "tomatoes.viewer.rating", "tomatoes.critic.rating")

                        .sort(Sort.descending("tomatoes.viewer.rating"))
                        .limit(3));
    }
}
