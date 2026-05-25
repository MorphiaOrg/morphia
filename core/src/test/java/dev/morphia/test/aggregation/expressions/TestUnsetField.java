package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Miscellaneous.getField;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.Miscellaneous.unsetField;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;

public class TestUnsetField extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/unsetField/example1
     * 
     */
    @Test
    @DisplayName("Remove Fields that Contain Periods (``.``)")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(unsetField("price.usd", ROOT))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/unsetField/example2
     * 
     */
    @Test
    @DisplayName("Remove Fields that Start with a Dollar Sign (``$``)")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(unsetField(literal("$price"), ROOT))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/unsetField/example3
     * 
     */
    @Test
    @DisplayName("Remove A Subfield")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation
                .pipeline(replaceWith(setField("price", ROOT, unsetField("euro", getField("price"))))));
    }

}
