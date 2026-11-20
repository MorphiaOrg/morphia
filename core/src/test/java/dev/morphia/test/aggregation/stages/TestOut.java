package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.models.Author;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Out.out;

public class TestOut extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/out/outputToSameDatabase
     * 
     */
    @Test
    @DisplayName("Output to Same Database")
    public void testOutputToSameDatabase() {
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation
                .pipeline(group(id("$author")).field("books", push().single("$title")), out(Author.class)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/out/outputToADifferentDatabase
     * 
     */
    @Test
    @DisplayName("Output to a Different Database")
    public void testOutputToADifferentDatabase() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push().single("$title")),
                        out(Author.class).database("reporting")));
    }
}
