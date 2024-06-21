package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.documentNumber;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestDocumentNumber extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/documentNumber/example1
     * 
     */
    @Test(testName = "Document Number for Each State")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                                .output(output("documentNumberForState").operator(documentNumber()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/documentNumber/example2
     * 
     */
    @Test(testName = "Document Number for Duplicate, Null, and Missing Values")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                                .output(output("documentNumberForState").operator(documentNumber()))));
    }

}
