package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.GraphLookup.graphLookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;

public class TestGraphLookup extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, false, (aggregation) -> aggregation.pipeline(
                graphLookup(EXAMPLE_TEST_COLLECTION)
                        .startWith("$reportsTo")
                        .connectFromField("reportsTo")
                        .connectToField("name")
                        .as("reportingHierarchy")));
    }

    @Test
    public void testExample2() {
        loadData("airports", 2);

        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                graphLookup("airports")
                        .startWith("$nearestAirport")
                        .connectFromField("connects")
                        .connectToField("airport")
                        .maxDepth(2)
                        .depthField("numConnections")
                        .as("destinations")));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                match(eq("name", "Tanya Jordan")),
                graphLookup(EXAMPLE_TEST_COLLECTION)
                        .startWith("$friends")
                        .connectFromField("friends")
                        .connectToField("name")
                        .as("golfers")
                        .restrict(eq("hobbies", "golf")),
                project()
                        .include("name")
                        .include("friends")
                        .include("connections who play golf", "$golfers.name")

        ));
    }

}
