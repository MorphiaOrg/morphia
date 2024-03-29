package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.RegexExpression;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.expressions.StringExpressions.split;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestSplit extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> {
            RegexExpression regex = regexMatch("$city_state").pattern("[A-Z]{2}");
            return aggregation.pipeline(
                    project()
                            .include("city_state", split("$city", ", "))
                            .include("qty"),
                    unwind("city_state"),
                    match(Filters.regex("city_state", "[A-Z]{2}")),
                    group(id()
                            .field("state", "$city_state"))
                            .field("total_qty", sum("$qty")),
                    sort()
                            .descending("total_qty"));
        });
    }

}
