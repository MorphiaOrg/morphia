package dev.morphia.geo;

import org.bson.Document;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static dev.morphia.geo.GeoJson.lineString;
import static dev.morphia.geo.GeoJson.multiPolygon;
import static dev.morphia.geo.GeoJson.point;
import static dev.morphia.geo.GeoJson.polygon;

public class GeometryCollectionTest extends TestBase {
    @Test
    public void shouldCorrectlySerialiseLineStringsInGeometryCollection() {
        // given
        LineString lineString = lineString(point(1, 2), point(3, 5), point(19, 13));
        GeometryCollection geometryCollection = GeoJson.geometryCollection(lineString);
        getMorphia().getMapper().addMappedClass(Point.class);

        // when
        Document dbObject = getMorphia().toDocument(geometryCollection);

        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + "  type: 'GeometryCollection', "
                                                              + "  geometries: "
                                                              + "  ["
                                                              + "    {"
                                                              + "     type: 'LineString', "
                                                              + "     coordinates: [ [ 2.0,  1.0],"
                                                              + "                    [ 5.0,  3.0],"
                                                              + "                    [13.0, 19.0] ]"
                                                              + "    },"
                                                              + "  ]"
                                                              + "}"));
    }

    @Test
    public void shouldCorrectlySerialiseMultiPointsInGeometryCollection() {
        // given
        MultiPoint multiPoint = GeoJson.multiPoint(point(1, 2), point(3, 5), point(19, 13));
        GeometryCollection geometryCollection = GeoJson.geometryCollection(multiPoint);

        // when
        Document dbObject = getMorphia().toDocument(geometryCollection);

        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + "  type: 'GeometryCollection', "
                                                              + "  geometries: "
                                                              + "  ["
                                                              + "    {"
                                                              + "     type: 'MultiPoint', "
                                                              + "     coordinates: [ [ 2.0,  1.0],"
                                                              + "                    [ 5.0,  3.0],"
                                                              + "                    [13.0, 19.0] ]"
                                                              + "    },"
                                                              + "  ]"
                                                              + " }"
                                                              + "}"));
    }

    @Test
    public void shouldCorrectlySerialiseMultiPolygonsInGeometryCollection() {
        // given
        MultiPolygon multiPolygon = multiPolygon(polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0))),
                                                 polygon(lineString(point(1.2, 3.0), point(2.5, 4.5), point(6.7, 1.9), point(1.2, 3.0)),
                                                         lineString(point(3.5, 2.4), point(1.7, 2.8), point(3.5, 2.4))));
        GeometryCollection geometryCollection = GeoJson.geometryCollection(multiPolygon);

        // when
        Document dbObject = getMorphia().toDocument(geometryCollection);

        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + "  type: 'GeometryCollection', "
                                                              + "  geometries: "
                                                              + "  ["
                                                              + "    {"
                                                              + "     type: 'MultiPolygon', "
                                                              + "     coordinates: [ [ [ [ 2.0, 1.1],"
                                                              + "                        [ 3.5, 2.3],"
                                                              + "                        [ 1.0, 3.7],"
                                                              + "                        [ 2.0, 1.1],"
                                                              + "                      ]"
                                                              + "                    ],"
                                                              + "                    [ [ [ 3.0, 1.2],"
                                                              + "                        [ 4.5, 2.5],"
                                                              + "                        [ 1.9, 6.7],"
                                                              + "                        [ 3.0, 1.2] "
                                                              + "                      ],"
                                                              + "                      [ [ 2.4, 3.5],"
                                                              + "                        [ 2.8, 1.7],"
                                                              + "                        [ 2.4, 3.5] "
                                                              + "                      ],"
                                                              + "                    ]"
                                                              + "                  ]"
                                                              + "    }"
                                                              + "  ]"
                                                              + " }"
                                                              + "}"));
    }

    @Test
    public void shouldCorrectlySerialisePointsInGeometryCollection() {
        // given
        Point point = point(3.0, 7.0);
        GeometryCollection geometryCollection = GeoJson.geometryCollection(point);

        // when
        Document dbObject = getMorphia().toDocument(geometryCollection);

        // then use the underlying driver to ensure it was persisted correctly to the database
        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + "  type: 'GeometryCollection', "
                                                              + "  geometries: "
                                                              + "  ["
                                                              + "    {"
                                                              + "     type: 'Point', "
                                                              + "     coordinates: [7.0, 3.0]"
                                                              + "    }, "
                                                              + "  ]"
                                                              + "}"));
    }

    @Test
    public void shouldCorrectlySerialisePolygonsInGeometryCollection() {
        // given
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8),
                                                      point(2.2, 2.1)));
        GeometryCollection geometryCollection = GeoJson.geometryCollection(polygonWithHoles);

        // when
        Document dbObject = getMorphia().toDocument(geometryCollection);

        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + "  type: 'GeometryCollection', "
                                                              + "  geometries: "
                                                              + "  ["
                                                              + "    {"
                                                              + "     type: 'Polygon', "
                                                              + "     coordinates: "
                                                              + "       [ [ [ 2.0, 1.1],"
                                                              + "           [ 3.5, 2.3],"
                                                              + "           [ 1.0, 3.7],"
                                                              + "           [ 2.0, 1.1] "
                                                              + "         ],"
                                                              + "         [ [ 2.0, 1.5],"
                                                              + "           [ 2.0, 1.9],"
                                                              + "           [ 1.8, 1.9],"
                                                              + "           [ 2.0, 1.5] "
                                                              + "         ],"
                                                              + "         [ [ 2.1, 2.2],"
                                                              + "           [ 1.9, 2.4],"
                                                              + "           [ 1.7, 2.4],"
                                                              + "           [ 1.8, 2.1],"
                                                              + "           [ 2.1, 2.2] "
                                                              + "         ]"
                                                              + "       ]"
                                                              + "    },"
                                                              + "  ]"
                                                              + " }"
                                                              + "}"));
    }
}
