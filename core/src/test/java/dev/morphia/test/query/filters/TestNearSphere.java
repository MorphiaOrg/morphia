package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.nearSphere;

public class TestNearSphere extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/nearSphere/example1
     */
    @Test(testName = "Specify Center Point Using GeoJSON")
    public void testExample1() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(nearSphere("location", new Point(new Position(-73.9667, 40.78)))
                        .minDistance(1000.0).maxDistance(5000.0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/nearSphere/example2
     */
    @Test(testName = "Specify Center Point Using Legacy Coordinates")
    public void testExample2() {
        // legacy coordinates just won't be supported for now
        /*
         * testQuery(new QueryTestOptions().skipDataCheck(true), (query) ->
         * query.filter( nearSphere("location", new Point(new Position(-73.9667,
         * 40.78))) .maxDistance(0.10) ));
         */
    }
}