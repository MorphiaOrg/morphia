package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.polygon;

public class TestPolygon extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/polygon/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        Point[] point = new Point[] {
                new Point(new Position(0, 0)),
                new Point(new Position(3, 6)),
                new Point(new Position(6, 0)),
        };
        testQuery(new QueryTestOptions().skipDataCheck(true),
                (query) -> query.filter(
                        polygon("loc", point)));
    }
}