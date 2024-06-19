package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.center;

public class TestCenter extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/center/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(center("loc", new Point(new Position(-74, 40.74)), 10)));
    }
}