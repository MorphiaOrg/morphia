package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.ServerVersion.MDB51;
import static dev.morphia.test.ServerVersion.MDB53;

public class TestFill extends AggregationTest {
    @Test
    public void testConstantValue() {
        checkMinDriverVersion(4.2);

        testPipeline(MDB53, "constantValue", aggregation -> {
            return aggregation
                    .fill(fill()
                            .field("bootsSold", value(0))
                            .field("sandalsSold", value(0))
                            .field("sneakersSold", value(0)));
        });
    }

    @Test
    public void testDistinctPartitions() {
        checkMinDriverVersion(4.2);

        testPipeline(MDB53, "distinctPartitions", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("date"))
                            .partitionBy(document("restaurant", field("restaurant")))
                            .field("score", Method.LOCF));
        });
    }

    @Test
    public void testLastObserved() {
        checkMinDriverVersion(4.2);

        testPipeline(MDB53, "lastObserved", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("date"))
                            .field("score", Method.LOCF));
        });
    }

    @Test
    public void testLinearInterpolation() {
        testPipeline(MDB53, "linearInterpolation", aggregation -> {
            return aggregation
                    .fill(fill()
                            .sortBy(ascending("time"))
                            .field("price", Method.LINEAR));
        });
    }
}
