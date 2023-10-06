package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.sortArray;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.Sort.naturalAscending;
import static dev.morphia.test.ServerVersion.v52;

public class TestSortArray extends AggregationTest {
    @Test
    public void testField() {
        testPipeline(v52, "field", (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(field("team"), ascending("name"))));
        });

    }

    @Test
    public void testSubfield() {
        testPipeline(v52, "subfield", (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(field("team"), descending("address.city"))));
        });

    }

    @Test
    public void testMultipleFields() {
        testPipeline(v52, "multipleFields", (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(field("team"),
                                    descending("age"), ascending("name"))));
        });

    }

    @Test
    public void testArrayOfIntegers() {
        testPipeline(v52, "arrayOfIntegers", (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(array(value(1), value(4), value(1), value(6), value(12), value(5)),
                                    naturalAscending())));
        });

    }

    @Test
    public void testMixedTypes() {
        testPipeline(v52, "mixedTypes", (aggregation) -> {
            return aggregation
                    .project(project()
                            .suppressId()
                            .include("result", sortArray(array(
                                    20,
                                    4,
                                    document("a", value("Free")),
                                    6,
                                    21,
                                    5,
                                    "Gratis",
                                    document("a", value(null)),
                                    document("a", document("sale", value(true))
                                            .field("price", value(19))),
                                    10.23,
                                    document("a", value("On sale"))),
                                    naturalAscending())));
        });

    }

}
