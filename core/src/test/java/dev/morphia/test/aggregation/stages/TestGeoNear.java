package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.GeoNear.geoNear;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestGeoNear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/geoNear/example1
     * 
     */
    @Test(testName = "Maximum Distance")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true).orderMatters(false),
                (aggregation) -> aggregation.pipeline(geoNear(new Point(new Position(-73.99279, 40.719296)))
                        .distanceField("dist.calculated").maxDistance(2).query(eq("category", "Parks"))
                        .includeLocs("dist.location").spherical(true)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/geoNear/example2
     * 
     */
    @Test(testName = "Minimum Distance")
    public void testExample2() {
        // this example isn't representable in morphia as is
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/geoNear/example3
     * 
     */
    @Test(testName = "$geoNear with the ``let`` option")
    public void testExample3() {
        // let doesn't apply to morphia
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/geoNear/example4
     *
     */
    @Test(testName = "$geoNear with Bound ``let`` Option")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.v60).removeIds(true).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        lookup(EXAMPLE_TEST_COLLECTION).as("joinedField").let("pt", "$location")
                                .pipeline(geoNear("$$pt").distanceField("distance")),
                        match(eq("name", "Sara D. Roosevelt Park"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/geoNear/example3
     *
     */
    @Test(testName = "Specify Which Geospatial Index to Use")
    public void testExample5() {
        testPipeline(new ActionTestOptions().removeIds(true).orderMatters(false).skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(geoNear(new Point(new Position(-73.98142, 40.71782)))
                        .distanceField("dist.calculated").key("location").query(eq("category", "Parks")), limit(5)));
    }

}
