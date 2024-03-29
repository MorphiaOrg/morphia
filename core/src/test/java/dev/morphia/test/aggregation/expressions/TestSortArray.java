package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.sortArray;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.Sort.naturalAscending;
import static dev.morphia.test.ServerVersion.v52;

public class TestSortArray extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(v52, (aggregation) -> aggregation
                .pipeline(project()
                        .suppressId()
                        .include("result", sortArray("$team", ascending("name")))));

    }

    @Test
    public void testExample2() {
        testPipeline(v52, (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray("$team", descending("address.city"))));
        });

    }

    @Test
    public void testExample3() {
        testPipeline(v52, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("result",
                                sortArray("$team",
                                        descending("age"),
                                        ascending("name")))));
    }

    @Test
    public void testExample4() {
        testPipeline(v52, (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(array(1, 4, 1, 6, 12, 5),
                                    naturalAscending())));
        });

    }

    @Test
    public void testExample5() {
        testPipeline(v52, false, false, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("result", sortArray(
                                array(20, 4,
                                        document("a", "Free"),
                                        6, 21, 5, "Gratis",
                                        document("a", null),
                                        document("a", document("sale", true).field("price", 19)),
                                        10.23,
                                        document("a", "On sale")),
                                naturalAscending()))));

    }

}
