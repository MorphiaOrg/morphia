package dev.morphia.test.aggregation.stages;

import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
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

    @Test
    @DisplayName("Exclude an Embedded Field with Dot Notation")
    public void testExcludeAnEmbeddedFieldWithDotNotation() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().exclude("imdb.id").exclude("type"),
                limit(1)));
    }

    @Test
    @DisplayName("Exclude Fields from Output Documents")
    public void testExcludeFieldsFromOutputDocuments() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().exclude("rated"),
                limit(1)));
    }

    @Test
    @DisplayName("Include an Embedded Field with a Nested Document")
    public void testIncludeAnEmbeddedFieldWithANestedDocument() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().include("imdb", document().field("rating", 1)),
                limit(1)));
    }

    @Test
    @DisplayName("Include an Embedded Field with Dot Notation")
    public void testIncludeAnEmbeddedFieldWithDotNotation() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().include("imdb.rating"),
                limit(1)));
    }

    @Test
    @DisplayName("Include Computed Fields")
    public void testIncludeComputedFields() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().include("title").include("leadActor", elementAt("$cast", 0)).include("releaseYear", "$year"),
                limit(1)));
    }

    @Test
    @DisplayName("Project an Array of Fields")
    public void testProjectAnArrayOfFields() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().include("myArray", array("$year", "$runtime")),
                limit(1)));
    }

    @Test
    @DisplayName("Project an Array with a Missing Field")
    public void testProjectAnArrayWithAMissingField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(Filters.eq("title", "The Great Train Robbery")),
                project().include("myArray", array("$year", "$runtime", "$someField")),
                limit(1)));
    }
}
