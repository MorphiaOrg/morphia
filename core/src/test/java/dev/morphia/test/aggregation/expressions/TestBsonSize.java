package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DataSizeExpressions.bsonSize;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.test.ServerVersion.ANY;

public class TestBsonSize extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                project()
                        .include("name")
                        .include("object_size",
                                bsonSize(ROOT))));

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                group(id(value(null)))
                        .field("combined_object_size",
                                sum(bsonSize(ROOT)))));

    }

    @Test
    public void testExample3() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                project()
                        .include("name", field("name"))
                        .include("task_object_size",
                                bsonSize(value("$$CURRENT"))),
                sort()
                        .descending("task_object_size"),
                limit(1)));

    }
}
