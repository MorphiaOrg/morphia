package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.GraphLookup.graphLookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.eq;

public class TestGraphLookup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/graphLookup/example1
     * 
     */
    @Test(testName = "Within a Single Collection")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(true).orderMatters(false),
                (aggregation) -> aggregation.pipeline(graphLookup(EXAMPLE_TEST_COLLECTION).startWith("$reportsTo")
                        .connectFromField("reportsTo").connectToField("name").as("reportingHierarchy")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/graphLookup/example2
     * 
     */
    @Test(testName = "Across Multiple Collections")
    public void testExample2() {
        loadData("airports", 2);

        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(graphLookup("airports").startWith("$nearestAirport")
                        .connectFromField("connects").connectToField("airport").maxDepth(2).depthField("numConnections")
                        .as("destinations")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/graphLookup/example3
     * 
     */
    @Test(testName = "With a Query Filter")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(match(eq("name", "Tanya Jordan")),
                        graphLookup(EXAMPLE_TEST_COLLECTION).startWith("$friends").connectFromField("friends")
                                .connectToField("name").as("golfers").restrict(eq("hobbies", "golf")),
                        project().include("name").include("friends").include("connections who play golf",
                                "$golfers.name")

                ));
    }

}
