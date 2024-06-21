package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.Author;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Out.out;

public class TestOut extends AggregationTest {
    public TestOut() {
        skipDataCheck();
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/out/example1
     * 
     */
    @Test(testName = "Output to Same Database")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(group(id("$author")).field("books", push().single("$title")), out(Author.class)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/out/example2
     * 
     */
    @Test(testName = "Output to a Different Database")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push().single("$title")),
                        out(Author.class).database("reporting")));
    }
}
