package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestProject extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/project/includeSpecificFieldsInOutputDocuments
     * 
     */
    @Test
    @DisplayName("Include Specific Fields in Output Documents")
    public void testIncludeSpecificFieldsInOutputDocuments() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("title").include("author")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/suppressIdFieldInTheOutputDocuments
     * 
     */
    @Test
    @DisplayName("Suppress ``_id`` Field in the Output Documents")
    public void testSuppressIdFieldInTheOutputDocuments() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("title").include("author")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/excludeFieldsFromOutputDocuments
     * 
     */
    @Test
    @DisplayName("Exclude Fields from Output Documents")
    public void testExcludeFieldsFromOutputDocuments() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(project().exclude("lastModified")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/conditionallyExcludeFields
     * 
     */
    @Test
    @DisplayName("Conditionally Exclude Fields")
    public void testConditionallyExcludeFields() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("title").include("author.first").include("author.last")
                        .include("author.middle", condition(eq("", "$author.middle"), REMOVE, "$author.middle"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/includeSpecificFieldsFromEmbeddedDocuments
     * 
     */
    @Test
    @DisplayName("Include Specific Fields from Embedded Documents")
    public void testIncludeSpecificFieldsFromEmbeddedDocuments() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("stop.title")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/includeSpecificFieldsFromEmbeddedDocumentsAsStage
     *
     */
    @Test
    @DisplayName("Include Specific Fields from Embedded Documents")
    public void testIncludeSpecificFieldsFromEmbeddedDocumentsAsStage() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("stop.title")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/includeComputedFields
     *
     */
    @Test
    @DisplayName("Include Computed Fields")
    public void testIncludeComputedFields() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("title")
                .include("isbn", document().field("prefix", substrBytes("$isbn", 0, 3))
                        .field("group", substrBytes("$isbn", 3, 2)).field("publisher", substrBytes("$isbn", 5, 4))
                        .field("title", substrBytes("$isbn", 9, 3)).field("checkDigit", substrBytes("$isbn", 12, 1)))
                .include("lastName", "$author.last").include("copiesSold", "$copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/arrayIndexesAreUnsupported
     * 
     */
    @Test
    @DisplayName("Array Indexes are Unsupported")
    public void testArrayIndexesAreUnsupported() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("x", "$name")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/project/arrayIndexesAreUnsupportedFromShellInsert
     * 
     */
    @Test
    @DisplayName("Array Indexes are Unsupported")
    public void testArrayIndexesAreUnsupportedFromShellInsert() {
        // unsupported multiple examples here
        /*
         * testPipeline(new dev.morphia.test.util.ActionTestOptions(). removeIds(false),
         * (aggregation) -> aggregation.pipeline( project() .suppressId() .include("x",
         * "$name")));
         */
    }

}
