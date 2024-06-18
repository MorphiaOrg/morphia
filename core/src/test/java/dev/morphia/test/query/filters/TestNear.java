package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.near;

public class TestNear extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/near/example1
     */
    @Test(testName = "Query on GeoJSON Data")
    public void testExample1() {
        testQuery(new QueryTestOptions().skipDataCheck(true), (query) -> query.filter(
                near("location", new Point(new Position(-73.9667, 40.78))).minDistance(1000.0).maxDistance(5000.0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/near/example2
     */
    @Test(testName = "Query on Legacy Coordinates")
    public void testExample2() {
        // legacy coordinates just won't be supported for now
        /*
         * testQuery(new QueryTestOptions().skipDataCheck(true), (query) ->
         * query.filter( near("location", new Point(new Position(-73.9667, 40.78)))
         * .maxDistance(0.1) ));
         */
    }
}