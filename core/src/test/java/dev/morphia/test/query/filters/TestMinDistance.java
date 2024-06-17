package dev.morphia.test.query.filters;

import org.testng.annotations.Test;

public class TestMinDistance extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example1
     * 
     * db.places.find( { location: { $near : { $geometry: { type: "Point",
     * coordinates: [ -73.9667, 40.78 ] }, $minDistance: 1000, $maxDistance: 5000 }
     * } } )
     */
    @Test(testName = "Use with ``$near``")
    public void testExample1() {
        // already tested elsewhere
    }

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example2
     * 
     * db.places.find( { location: { $nearSphere: { $geometry: { type : "Point",
     * coordinates : [ -73.9667, 40.78 ] }, $minDistance: 1000, $maxDistance: 5000 }
     * } } )
     */
    @Test(testName = "Use with ``$nearSphere``")
    public void testExample2() {
        // legacy coordinates just won't be supported for now
    }
}