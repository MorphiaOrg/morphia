package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIfNull extends TemplatedTestBase {

    @BeforeMethod
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/ifNull/example1
     * 
     */
    @Test(testName = "Single Input Expression")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("item").include("description",
                ifNull().target("$description").replacement("Unspecified"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/ifNull/example2
     * 
     */
    @Test(testName = "Multiple Input Expressions")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("item").include("value",
                ifNull().input("$description", "$quantity").replacement("Unspecified"))));
    }

}
