package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.MathExpressions.median;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestMedian extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/median/useMedianAsAnAccumulator
     */
    @Test
    @DisplayName("Use |operatorName| as an Accumulator")
    public void testUseMedianAsAnAccumulator() {
        testPipeline(aggregation -> aggregation.pipeline(group().field("test01_median", median("$test01"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/median/useMedianInAProjectStage
     *
     */
    @Test
    @DisplayName("Use |operatorName| in a ``$project`` Stage")
    public void testUseMedianInAProjectStage() {
        testPipeline(aggregation -> aggregation.pipeline(project().suppressId().include("studentId").include("testMedians",
                median(array("$test01", "$test02", "$test03")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/median/useMedianInASetWindowFieldStage
     *
     */
    @Test
    @DisplayName("Use |operatorName| in a ``$setWindowField`` Stage")
    public void testUseMedianInASetWindowFieldStage() {
        testPipeline(aggregation -> aggregation.pipeline(
                setWindowFields().sortBy(ascending("test01"))
                        .output(output("test01_median").operator(median("$test01")).window().range(-3, 3)),
                project().suppressId().include("studentId").include("test01_median")));
    }
}
