package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.polygon;

public class TestPolygon extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/polygon/example1
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        Point[] point = new Point[] { new Point(new Position(0, 0)), new Point(new Position(3, 6)),
                new Point(new Position(6, 0)), };
        testQuery(new ActionTestOptions().skipDataCheck(true), (query) -> query.filter(polygon("loc", point)));
    }
}