package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.models.geo.GeoCity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;

import static dev.morphia.aggregation.stages.GeoNear.geoNear;

public class TestGeoNear extends AggregationTest {
    @Test
    public void testGeoNearWithSphericalGeometry() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        GeoCity london = new GeoCity("London", new Point(new Position(latitude, longitude)));
        getDs().save(london);
        GeoCity manchester = new GeoCity("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        GeoCity sevilla = new GeoCity("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        Iterator<GeoCity> cities = getDs().aggregate(GeoCity.class)
                .geoNear(geoNear(new double[] { latitude, longitude })
                        .distanceField("distance")
                        .spherical(true))
                .execute(GeoCity.class);

        // then
        Assert.assertTrue(cities.hasNext());
        Assert.assertEquals(london, cities.next());
        Assert.assertEquals(manchester, cities.next());
        Assert.assertEquals(sevilla, cities.next());
        Assert.assertFalse(cities.hasNext());
    }

}
