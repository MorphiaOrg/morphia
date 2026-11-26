package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.linearFill;
import static dev.morphia.aggregation.expressions.WindowExpressions.locf;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestLocf extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.2.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/locf/fillMissingValuesWithTheLastObservedValue
     * 
     */
    @Test
    @DisplayName("Fill Missing Values with the Last Observed Value")
    public void testFillMissingValuesWithTheLastObservedValue() {
        testPipeline((aggregation) -> aggregation.pipeline(
                setWindowFields().sortBy(ascending("time")).output(output("price").operator(locf("$price")))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/locf/useMultipleFillMethodsInASingleStage
     * 
     */
    @Test
    @DisplayName("Use Multiple Fill Methods in a Single Stage")
    public void testUseMultipleFillMethodsInASingleStage() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().sortBy(ascending("time")).output(
                output("linearFillPrice").operator(linearFill("$price")),
                output("locfPrice").operator(locf("$price")))));
    }

}
