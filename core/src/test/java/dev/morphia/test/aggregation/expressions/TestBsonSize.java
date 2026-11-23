package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DataSizeExpressions.bsonSize;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestBsonSize extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/bsonSize/returnSizesOfDocuments
     * 
     */
    @Test
    @DisplayName("Return Sizes of Documents")
    public void testReturnSizesOfDocuments() {
        testPipeline(
                aggregation -> aggregation.pipeline(project().include("name").include("object_size", bsonSize(ROOT))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/bsonSize/returnCombinedSizeOfAllDocumentsInACollection
     * 
     */
    @Test
    @DisplayName("Return Combined Size of All Documents in a Collection")
    public void testReturnCombinedSizeOfAllDocumentsInACollection() {
        testPipeline(aggregation -> aggregation
                .pipeline(group(id(null)).field("combined_object_size", sum(bsonSize(ROOT)))));

    }

}
