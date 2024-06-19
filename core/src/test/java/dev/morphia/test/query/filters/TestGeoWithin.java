package dev.morphia.test.query.filters;

import java.util.List;

import com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.geoWithin;

public class TestGeoWithin extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/geoWithin/example1
     */
    @Test(testName = "Within a Polygon")
    public void testExample1() {
        var points = List.of(new Position(0, 0), new Position(3, 6), new Position(6, 1), new Position(0, 0));
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(geoWithin("loc", new Polygon(points))));
    }

    /**
     * test data: dev/morphia/test/query/filters/geoWithin/example2
     */
    @Test(testName = "Within a \"Big\" Polygon")
    public void testExample2() {
        var coords = new PolygonCoordinates(List.of(new Position(-100, 60), new Position(-100, 0),
                new Position(-100, -60), new Position(100, -60), new Position(100, 60), new Position(-100, 60)));
        testQuery(new ActionTestOptions().skipDataCheck(true), (query) -> query.filter(geoWithin("loc",
                new Polygon(new NamedCoordinateReferenceSystem("urn:x-mongodb:crs:strictwinding:EPSG:4326"), coords))));
    }
}