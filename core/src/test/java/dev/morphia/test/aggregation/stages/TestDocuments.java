package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;

public class TestDocuments extends TemplatedTestBase {
    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("6.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/documents/useADocumentsStageInALookupStage
     * 
     */
    @Test
    @DisplayName("Use a ``$documents`` Stage in a ``$lookup`` Stage")
    public void testUseADocumentsStageInALookupStage() {
        testPipeline(aggregation -> {
            return aggregation.pipeline(match(),
                    lookup().localField("zip").foreignField("zip_id").as("city_state")
                            .pipeline(documents(document("zip_id", 94301).field("name", "Palo Alto, CA"),
                                    document("zip_id", 10019).field("name", "New York, NY"))));

        });
    }
}
