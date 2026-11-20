package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.comment;
import static dev.morphia.query.filters.Filters.mod;

public class TestComment extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/comment/attachACommentToFind
     */
    @Test
    @DisplayName("Attach a Comment to ``find``")
    public void testAttachACommentToFind() {
        ActionTestOptions options = new ActionTestOptions().skipDataCheck(true);
        testQuery(options, (query) -> query.filter(mod("x", 2, 0), comment("Find even values.")));
    }

    /**
     * test data: dev/morphia/test/query/filters/comment/attachACommentToAnAggregationExpression
     */
    @Test
    @DisplayName("Attach a Comment to an Aggregation Expression")
    public void testAttachACommentToAnAggregationExpression() {
        // this example is for an aggregation
    }
}