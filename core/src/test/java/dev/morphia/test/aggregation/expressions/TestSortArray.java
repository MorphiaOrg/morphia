package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.sortArray;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.Sort.naturalAscending;

public class TestSortArray extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/sortOnASubfield
     * 
     */
    @Test
    @DisplayName("Sort on a Subfield")
    public void testSortOnASubfield() {
        testPipeline(new ActionTestOptions(), (aggregation) -> {
            return aggregation
                    .pipeline(project().suppressId().include("result", sortArray("$team", descending("address.city"))));
        });

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/sortAnArrayOfIntegers
     * 
     */
    @Test
    @DisplayName("Sort an Array of Integers")
    public void testSortAnArrayOfIntegers() {
        testPipeline(new ActionTestOptions(), (aggregation) -> {
            return aggregation.pipeline(
                    project().suppressId().include("result", sortArray(array(1, 4, 1, 6, 12, 5), naturalAscending())));
        });

    }

}
