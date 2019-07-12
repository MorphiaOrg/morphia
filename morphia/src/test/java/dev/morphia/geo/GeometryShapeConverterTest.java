package dev.morphia.geo;

import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static dev.morphia.geo.GeoJson.lineString;
import static dev.morphia.geo.GeoJson.point;

public class GeometryShapeConverterTest extends TestBase {
    @Test
    public void shouldConvertAnEntityWithAPolygonGeoJsonType() {
        // given
        GeometryShapeConverter.PolygonConverter converter = new GeometryShapeConverter.PolygonConverter();
//        converter.setMapper(getMapper());
        Polygon polygon = GeoJson.polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                          lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                          lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));

        // when
        Object encodedPolygon = converter.encode(polygon, null);

        // then
        assertThat(encodedPolygon.toString(), JSONMatcher.jsonEqual("  {"
                                                                    + "  type: 'Polygon', "
                                                                    + "  coordinates: "
                                                                    + "    [ [ [ 2.0, 1.1],"
                                                                    + "        [ 3.5, 2.3],"
                                                                    + "        [ 1.0, 3.7],"
                                                                    + "        [ 2.0, 1.1] "
                                                                    + "      ],"
                                                                    + "      [ [ 2.0, 1.5],"
                                                                    + "        [ 2.0, 1.9],"
                                                                    + "        [ 1.8, 1.9],"
                                                                    + "        [ 2.0, 1.5] "
                                                                    + "      ],"
                                                                    + "      [ [ 2.1, 2.2],"
                                                                    + "        [ 1.9, 2.4],"
                                                                    + "        [ 1.7, 2.4],"
                                                                    + "        [ 1.8, 2.1],"
                                                                    + "        [ 2.1, 2.2] "
                                                                    + "      ]"
                                                                    + "    ]"
                                                                    + "}"));
    }

    @Test
    public void shouldCorrectlyEncodePointsIntoEntityDocument() {
        // given
        GeometryShapeConverter.PointConverter pointConverter = new GeometryShapeConverter.PointConverter();
//        pointConverter.setMapper(getMapper());

        Point point = point(3.0, 7.0);

        // when
        Object dbObject = pointConverter.encode(point, null);


        // then
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  { "
                                                              + "  type : 'Point' , "
                                                              + "  coordinates : [7, 3]"
                                                              + "}"));
    }

    @Test
    public void shouldEncodeAnEntityWithAMultiLineStringGeoJsonType() {
        // given
        GeometryShapeConverter.MultiLineStringConverter converter = new GeometryShapeConverter.MultiLineStringConverter();
//        converter.setMapper(getMapper());
        MultiLineString multiLineString = GeoJson.multiLineString(lineString(point(1, 2), point(3, 5), point(19, 13)),
                                                                  lineString(point(1.5, 2.0),
                                                                             point(1.9, 2.0),
                                                                             point(1.9, 1.8),
                                                                             point(1.5, 2.0)));

        // when
        Object encoded = converter.encode(multiLineString, null);

        // then
        assertThat(encoded.toString(), JSONMatcher.jsonEqual("  {"
                                                             + "  type: 'MultiLineString', "
                                                             + "  coordinates: "
                                                             + "     [ [ [ 2.0,  1.0],"
                                                             + "         [ 5.0,  3.0],"
                                                             + "         [13.0, 19.0] "
                                                             + "       ], "
                                                             + "       [ [ 2.0, 1.5],"
                                                             + "         [ 2.0, 1.9],"
                                                             + "         [ 1.8, 1.9],"
                                                             + "         [ 2.0, 1.5] "
                                                             + "       ]"
                                                             + "     ]"
                                                             + "}"));
    }

    @Test
    public void shouldEncodeAnEntityWithAMultiPolygonGeoJsonType() {
        // given
        GeometryShapeConverter.MultiPolygonConverter converter = new GeometryShapeConverter.MultiPolygonConverter();
//        converter.setMapper(getMapper());
        Polygon polygonWithHoles = GeoJson.polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                                   lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                                   lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8),
                                                              point(2.2, 2.1)));
        MultiPolygon multiPolygon = GeoJson.multiPolygon(GeoJson.polygon(point(1.1, 2.0),
                                                                         point(2.3, 3.5),
                                                                         point(3.7, 1.0),
                                                                         point(1.1, 2.0)),
                                                         polygonWithHoles);

        // when
        Object encoded = converter.encode(multiPolygon, null);

        // then
        assertThat(encoded.toString(), JSONMatcher.jsonEqual("  {"
                                                             + "  type: 'MultiPolygon', "
                                                             + "  coordinates: [ [ [ [ 2.0, 1.1],"
                                                             + "                     [ 3.5, 2.3],"
                                                             + "                     [ 1.0, 3.7],"
                                                             + "                     [ 2.0, 1.1],"
                                                             + "                   ]"
                                                             + "                 ],"
                                                             + "                 [ [ [ 2.0, 1.1],"
                                                             + "                     [ 3.5, 2.3],"
                                                             + "                     [ 1.0, 3.7],"
                                                             + "                     [ 2.0, 1.1] "
                                                             + "                   ],"
                                                             + "                   [ [ 2.0, 1.5],"
                                                             + "                     [ 2.0, 1.9],"
                                                             + "                     [ 1.8, 1.9],"
                                                             + "                     [ 2.0, 1.5] "
                                                             + "                   ],"
                                                             + "                   [ [ 2.1, 2.2],"
                                                             + "                     [ 1.9, 2.4],"
                                                             + "                     [ 1.7, 2.4],"
                                                             + "                     [ 1.8, 2.1],"
                                                             + "                     [ 2.1, 2.2] "
                                                             + "                   ]"
                                                             + "                 ]"
                                                             + "               ]"
                                                             + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithALineStringGeoJsonType() {
        // given
        GeometryShapeConverter.LineStringConverter converter = new GeometryShapeConverter.LineStringConverter();
//        converter.setMapper(getMapper());
        LineString lineString = lineString(point(1, 2), point(3, 5), point(19, 13));

        // when
        Object encodedLineString = converter.encode(lineString, null);

        // then
        assertThat(encodedLineString.toString(), JSONMatcher.jsonEqual("  {"
                                                                       + "  type: 'LineString', "
                                                                       + "  coordinates: [ [ 2.0,  1.0],"
                                                                       + "                 [ 5.0,  3.0],"
                                                                       + "                 [13.0, 19.0] ]"
                                                                       + "}"));
    }
}
