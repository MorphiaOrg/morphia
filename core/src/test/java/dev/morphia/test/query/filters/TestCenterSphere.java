package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.centerSphere;

public class TestCenterSphere extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/centerSphere/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(centerSphere("loc", new Point(new Position(-88, 30)), 0.0025232135648)));
    }
}