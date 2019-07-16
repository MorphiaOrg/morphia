package dev.morphia.geo;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class GeometryQueryConverterTest extends TestBase {
    @Test
    public void shouldCorrectlyEncodePointsIntoQueryDocument() {
        // given
        GeometryQueryConverter geometryConverter = new GeometryQueryConverter(getMapper());
        //        geometryConverter.setMapper(getMapper());

        Point point = new Point(new Position(3.0, 7.0));

        // when
        Object dbObject = geometryConverter.encode(point, null);


        // then
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  { $geometry : "
                                                              + "  { type : 'Point' , "
                                                              + "    coordinates : " + point.getCoordinates()
                                                              + "  }"
                                                              + "}"));
    }
}
