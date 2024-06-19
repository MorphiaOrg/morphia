package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.box;

public class TestBox extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/box/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        Point bottomLeft = new Point(new Position(0, 0));
        Point upperRight = new Point(new Position(100, 100));
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(box("loc", bottomLeft, upperRight)));
    }
}