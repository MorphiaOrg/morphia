package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.range;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ArrayExpressions.zip;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestZip extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("transposed", zip(
                                elementAt(field("matrix"), value(0)),
                                elementAt(field("matrix"), value(1)),
                                elementAt(field("matrix"), value(2))))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> {
            return aggregation.pipeline(
                    project()
                            .suppressId()
                            .include("pages", filter(
                                    zip(field("pages"), range(value(0), size(field("pages")))),
                                    let(gte(value("$$page.reviews"), value(1)))
                                            .variable("page",
                                                    elementAt(value("$$pageWithIndex"), value(0))))
                                    .as("pageWithIndex")));
        });
    }

}
