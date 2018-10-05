package xyz.morphia.geo;

import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static xyz.morphia.geo.GeoJson.point;

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
