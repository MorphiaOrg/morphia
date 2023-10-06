package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.DriverVersion.v42;
import static dev.morphia.test.ServerVersion.v53;

public class TestFill extends AggregationTest {
    @Test
    public void testConstantValue() {
        checkMinDriverVersion(v42);

        testPipeline(v53, "constantValue", aggregation -> {
            return aggregation
                    .fill(fill()
                            .field("bootsSold", value(0))
                            .field("sandalsSold", value(0))
                            .field("sneakersSold", value(0)));
        });
    }

    @Test
    public void testDistinctPartitions() {
        checkMinDriverVersion(v42);

        testPipeline(v53, "distinctPartitions", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("date"))
                            .partitionBy(document("restaurant", field("restaurant")))
                            .field("score", Method.LOCF));
        });
    }

    @Test
    public void testLastObserved() {
        checkMinDriverVersion(v42);

        testPipeline(v53, "lastObserved", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("date"))
                            .field("score", Method.LOCF));
        });
    }

    @Test
    public void testLinearInterpolation() {
        testPipeline(v53, "linearInterpolation", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("time"))
                            .field("price", Method.LINEAR));
        });
    }
}
