package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.GraphLookup.graphLookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;

public class TestGraphLookup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/graphLookup/withinASingleCollection
     * 
     */
    @Test
    @DisplayName("Within a Single Collection")
    public void testWithinASingleCollection() {
        testPipeline((aggregation) -> aggregation.pipeline(graphLookup("employees").startWith("$reportsTo")
                .connectFromField("reportsTo").connectToField("name").as("reportingHierarchy")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/graphLookup/withAQueryFilter
     * 
     */
    @Test
    @DisplayName("With a Query Filter")
    public void testWithAQueryFilter() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(eq("name", "Tanya Jordan")),
                graphLookup("people").startWith("$friends").connectFromField("friends")
                        .connectToField("name").as("golfers").restrict(eq("hobbies", "golf")),
                project().include("name").include("friends").include("connections who play golf", "$golfers.name")

        ));
    }

}
