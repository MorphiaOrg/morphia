package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.documentNumber;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestDocumentNumber extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/documentNumber/example1
     * 
     */
    @Test(testName = "Document Number for Each State")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
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
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                                .output(output("documentNumberForState").operator(documentNumber()))));
    }

}
