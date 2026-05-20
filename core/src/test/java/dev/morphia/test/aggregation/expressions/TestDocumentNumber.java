package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.documentNumber;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestDocumentNumber extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/documentNumber/example1
     * 
     */
    @Test
    @DisplayName("Document Number for Each State")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("documentNumberForState").operator(documentNumber()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/documentNumber/example2
     * 
     */
    @Test
    @DisplayName("Document Number for Duplicate, Null, and Missing Values")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("documentNumberForState").operator(documentNumber()))));
    }

}
