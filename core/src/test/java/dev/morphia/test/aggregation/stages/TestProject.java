package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
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

}
