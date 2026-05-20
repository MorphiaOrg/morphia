package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.geoIntersects;
import static java.util.List.of;

public class TestGeoIntersects extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/geoIntersects/example1
     */
    @Test
    @DisplayName("Intersects a Polygon")
    public void testExample1() {
        testQuery(new ActionTestOptions().skipDataCheck(true), (query) -> query.filter(geoIntersects("loc",
                new Polygon(of(new Position(0, 0), new Position(3, 6), new Position(6, 1), new Position(0, 0))))));
    }

    /**
     * test data: dev/morphia/test/query/filters/geoIntersects/example2
     */
    @Test
    @DisplayName("Intersects a \"Big\" Polygon")
    public void testExample2() {
        var exterior = new PolygonCoordinates(of(new Position(-100, 60), new Position(-100, 0), new Position(-100, -60),
                new Position(100, -60), new Position(100, 60), new Position(-100, 60)));
        testQuery(new ActionTestOptions().skipDataCheck(true), (query) -> query.filter(geoIntersects("loc", new Polygon(
                new NamedCoordinateReferenceSystem("urn:x-mongodb:crs:strictwinding:EPSG:4326"), exterior))));
    }
}