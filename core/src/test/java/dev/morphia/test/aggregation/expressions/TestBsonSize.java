package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DataSizeExpressions.bsonSize;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestBsonSize extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/bsonSize/example1
     * 
     */
    @Test(testName = "Return Sizes of Documents")
    public void testExample1() {
        testPipeline(
                aggregation -> aggregation.pipeline(project().include("name").include("object_size", bsonSize(ROOT))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bsonSize/example2
     * 
     */
    @Test(testName = "Return Combined Size of All Documents in a Collection")
    public void testExample2() {
        testPipeline(aggregation -> aggregation
                .pipeline(group(id(null)).field("combined_object_size", sum(bsonSize(ROOT)))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bsonSize/example3
     * 
     */
    @Test(testName = "Return Document with Largest Specified Field")
    public void testExample3() {
        testPipeline(aggregation -> aggregation.pipeline(
                project().include("name", "$name").include("task_object_size", bsonSize("$$CURRENT")),
                sort().descending("task_object_size"), limit(1)));

    }
}
