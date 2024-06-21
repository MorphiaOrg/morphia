package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.sortArray;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.Sort.naturalAscending;
import static dev.morphia.test.ServerVersion.v52;

public class TestSortArray extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/example1
     * 
     */
    @Test(testName = "Sort on a Field ")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v52), (aggregation) -> aggregation
                .pipeline(project().suppressId().include("result", sortArray("$team", ascending("name")))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/example2
     * 
     */
    @Test(testName = "Sort on a Subfield")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v52), (aggregation) -> {
            return aggregation
                    .project(project().suppressId().include("result", sortArray("$team", descending("address.city"))));
        });

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/example3
     * 
     */
    @Test(testName = "Sort on Multiple Fields")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v52), (aggregation) -> aggregation.pipeline(
                project().suppressId().include("result", sortArray("$team", descending("age"), ascending("name")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/example4
     * 
     */
    @Test(testName = "Sort an Array of Integers")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(v52), (aggregation) -> {
            return aggregation.project(
                    project().suppressId().include("result", sortArray(array(1, 4, 1, 6, 12, 5), naturalAscending())));
        });

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sortArray/example5
     * 
     */
    @Test(testName = "Sort on Mixed Type Fields")
    public void testExample5() {
        testPipeline(
                new ActionTestOptions().serverVersion(v52).orderMatters(false), (
                        aggregation) -> aggregation
                                .pipeline(
                                        project().suppressId()
                                                .include("result",
                                                        sortArray(
                                                                array(20, 4, document("a", "Free"), 6, 21, 5, "Gratis",
                                                                        document("a", null),
                                                                        document("a",
                                                                                document("sale", true).field("price",
                                                                                        19)),
                                                                        10.23, document("a", "On sale")),
                                                                naturalAscending()))));

    }

}
