package org.mongodb.morphia.query;

import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.geo.City;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.geo.GeoJson.point;
import static org.mongodb.morphia.geo.PointBuilder.pointBuilder;

public class GeoQueriesTest extends TestBase {
    @Override
    @Before
    public void setUp() {
        // this whole test class is designed for "modern" geo queries
        checkMinServerVersion(2.4);
        super.setUp();
    }

    @Test
    public void shouldFindCitiesCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        Datastore datastore = getDs();
        City london = new City("London", point(latitude, longitude));
        datastore.save(london);
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        datastore.save(manchester);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        datastore.save(sevilla);

        getDs().ensureIndexes();

        // when
        List<City> citiesOrderedByDistanceFromLondon = datastore.find(City.class)
                                                                .field("location")
                                                                .near(pointBuilder().latitude(latitude)
                                                                                    .longitude(longitude).build(), 200000)
                                                                .asList();

        // then
        assertThat(citiesOrderedByDistanceFromLondon.size(), is(1));
        assertThat(citiesOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindCitiesOrderByDistanceFromAGivenPoint() {
        // given
        double latitudeLondon = 51.5286416;
        double longitudeLondon = -0.1015987;
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City london = new City("London", point(latitudeLondon, longitudeLondon));
        getDs().save(london);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        List<City> citiesOrderedByDistanceFromLondon = getDs().find(City.class)
                                                              .field("location")
                                                              .near(pointBuilder().latitude(latitudeLondon)
                                                                                  .longitude(latitudeLondon).build())
                                                              .asList();

        // then
        assertThat(citiesOrderedByDistanceFromLondon.size(), is(3));
        assertThat(citiesOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(citiesOrderedByDistanceFromLondon.get(1), is(manchester));
        assertThat(citiesOrderedByDistanceFromLondon.get(2), is(sevilla));
    }

}
