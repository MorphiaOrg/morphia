package dev.morphia.query;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.geo.model.City;
import org.junit.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.near;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoQueriesTest extends TestBase {
    @Test
    public void shouldFindCitiesCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        Datastore datastore = getDs();
        City london = new City("London", new Point(new Position(latitude, longitude)));
        datastore.save(london);
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        datastore.save(manchester);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        datastore.save(sevilla);

        getDs().ensureIndexes();

        // when
        List<City> cities = datastore.find(City.class)
                                     .filter(near("location", new Point(new Position(latitude, longitude)))
                                                 .maxDistance(200000.0))
                                     .execute()
                                     .toList();

        // then
        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));
    }
}
