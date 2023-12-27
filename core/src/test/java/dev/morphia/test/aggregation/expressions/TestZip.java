package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.range;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ArrayExpressions.zip;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestZip extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("transposed", zip(
                                elementAt("$matrix", 0),
                                elementAt("$matrix", 1),
                                elementAt("$matrix", 2)))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> {
            return aggregation.pipeline(
                    project()
                            .suppressId()
                            .include("pages", filter(
                                    zip("$pages", range(0, size("$pages"))),
                                    let(gte("$$page.reviews", 1))
                                            .variable("page",
                                                    elementAt("$$pageWithIndex", 0)))
                                    .as("pageWithIndex")));
        });
    }

}
