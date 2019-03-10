package dev.morphia.geo;

import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static dev.morphia.geo.GeoJson.point;

public class GeometryQueryConverterTest extends TestBase {
    @Test
    public void shouldCorrectlyEncodePointsIntoQueryDocument() {
        // given
        GeometryQueryConverter geometryConverter = new GeometryQueryConverter(getMorphia().getMapper());
        geometryConverter.setMapper(getMorphia().getMapper());

        Point point = point(3.0, 7.0);

        // when
        Object dbObject = geometryConverter.encode(point);


        // then
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  { $geometry : "
                                                              + "  { type : 'Point' , "
                                                              + "    coordinates : " + point.getCoordinates()
                                                              + "  }"
                                                              + "}"));
    }
}
