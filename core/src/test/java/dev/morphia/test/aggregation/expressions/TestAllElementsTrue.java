package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.SetExpressions.allElementsTrue;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAllElementsTrue extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, false, aggregation -> aggregation
                .project(Projection.project()
                        .suppressId()
                        .include("responses")
                        .include("isAllTrue", allElementsTrue(field("responses")))));

    }
}
