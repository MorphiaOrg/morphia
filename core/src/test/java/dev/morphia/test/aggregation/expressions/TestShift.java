package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.descending;

public class TestShift extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/shift/example1
     * 
     */
    @Test
    @DisplayName("Shift Using a Positive Integer")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState").operator(shift("$quantity", 1, "Not available")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/shift/example2
     * 
     */
    @Test
    @DisplayName("Shift Using a Negative Integer")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                        .output(output("shiftQuantityForState").operator(shift("$quantity", -1, "Not available")))));
    }

}
