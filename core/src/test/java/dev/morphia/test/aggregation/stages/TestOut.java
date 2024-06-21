package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.models.Author;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Out.out;

public class TestOut extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/out/example1
     * 
     */
    @Test(testName = "Output to Same Database")
    public void testExample1() {
        testPipeline(
                new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                        .skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push().single("$title")),
                        out(Author.class)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/out/example2
     * 
     */
    @Test(testName = "Output to a Different Database")
    public void testExample2() {
        testPipeline(
                new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                        .skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push().single("$title")),
                        out(Author.class).database("reporting")));
    }
}
