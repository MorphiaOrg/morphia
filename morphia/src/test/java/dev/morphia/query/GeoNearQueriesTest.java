package dev.morphia.query;

import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.geo.AllTheThings;
import dev.morphia.geo.Area;
import dev.morphia.geo.City;
import dev.morphia.geo.Point;
import dev.morphia.geo.Regions;
import dev.morphia.geo.Route;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static dev.morphia.geo.GeoJson.geometryCollection;
import static dev.morphia.geo.GeoJson.lineString;
import static dev.morphia.geo.GeoJson.multiPoint;
import static dev.morphia.geo.GeoJson.multiPolygon;
import static dev.morphia.geo.GeoJson.point;
import static dev.morphia.geo.GeoJson.polygon;
import static dev.morphia.geo.PointBuilder.pointBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoNearQueriesTest extends TestBase {
    @Override
    @Before
    public void setUp() {
        // this whole test class is designed for "modern" geo queries
        assumeMinServerVersion(2.4);
        super.setUp();
    }

    @Test
    public void shouldFindAreasCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        Area sevilla = new Area("Spain", polygon(pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build(),
                                                 pointBuilder().latitude(37.40341208875179).longitude(-5.9643941558897495).build(),
                                                 pointBuilder().latitude(37.40297396667302).longitude(-5.970452763140202).build(),
                                                 pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build())
        );
        getDs().save(sevilla);
        Area newYork = new Area("New York", polygon(pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build(),
                                                    pointBuilder().latitude(40.7636824529618).longitude(-73.98049869574606).build(),
                                                    pointBuilder().latitude(40.76962974853814).longitude(-73.97964206524193).build(),
                                                    pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build()));
        getDs().save(newYork);
        Area london = new Area("London", polygon(pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build(),
                                                 pointBuilder().latitude(51.50802478194237).longitude(-0.21474729292094707).build(),
                                                 pointBuilder().latitude(51.5086863655597).longitude(-0.20895397290587425).build(),
                                                 pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build()));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = toList(getDs().find(Area.class)
                                                              .field("area")
                                                              .near(pointBuilder()
                                                                        .latitude(51.5286416)
                                                                        .longitude(-0.1015987).build(), 20000)
                                                              .find());

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(1));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindAreasOrderedByDistanceFromAGivenPoint() {
        // given
        Area sevilla = new Area("Spain", polygon(pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build(),
                                                 pointBuilder().latitude(37.40341208875179).longitude(-5.9643941558897495).build(),
                                                 pointBuilder().latitude(37.40297396667302).longitude(-5.970452763140202).build(),
                                                 pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build())
        );
        getDs().save(sevilla);
        Area newYork = new Area("New York", polygon(pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build(),
                                                    pointBuilder().latitude(40.7636824529618).longitude(-73.98049869574606).build(),
                                                    pointBuilder().latitude(40.76962974853814).longitude(-73.97964206524193).build(),
                                                    pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build()));
        getDs().save(newYork);
        Area london = new Area("London", polygon(pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build(),
                                                 pointBuilder().latitude(51.50802478194237).longitude(-0.21474729292094707).build(),
                                                 pointBuilder().latitude(51.5086863655597).longitude(-0.20895397290587425).build(),
                                                 pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build()));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = toList(getDs().find(Area.class)
                                                              .field("area")
                                                              .near(pointBuilder()
                                                                        .latitude(51.5286416)
                                                                        .longitude(-0.1015987).build())
                                                              .find());

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(3));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(routesOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(routesOrderedByDistanceFromLondon.get(2), is(newYork));
    }

    @Test
    public void shouldFindNearAPoint() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        Datastore datastore = getDs();
        City london = new City("London", point(51.5286416, -0.1015987));
        datastore.save(london);
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        datastore.save(manchester);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        datastore.save(sevilla);

        getDs().ensureIndexes();

        final Point searchPoint = pointBuilder().latitude(50).longitude(0.1278)
                                                .build();
        List<City> cities = toList(datastore.find(City.class)
                                            .field("location")
                                            .near(searchPoint, 200000)
                                            .find());

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        cities = toList(datastore.find(City.class)
                                            .field("location")
                                            .nearSphere(searchPoint, 200000D, null)
                                            .find());

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        assertThat(toList(datastore.find(City.class)
                                   .field("location")
                                   .near(searchPoint, 200000D, 195000D)
                                   .find()).size(), is(0));

        assertThat(toList(datastore.find(City.class)
                                   .field("location")
                                   .nearSphere(searchPoint, 200000D, 195000D)
                                   .find()).size(), is(0));
    }

    @Test
    public void shouldFindCitiesOrderedByDistance() {
        double latitudeLondon = 51.5286416;
        double longitudeLondon = -0.1015987;
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City london = new City("London", point(latitudeLondon, longitudeLondon));
        getDs().save(london);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        List<City> cities = toList(getDs().find(City.class)
                                          .field("location")
                                          .near(pointBuilder()
                                                    .latitude(latitudeLondon)
                                                    .longitude(longitudeLondon).build())
                                          .find());

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));

        cities = toList(getDs().find(City.class)
                                          .field("location")
                                          .nearSphere(pointBuilder()
                                                    .latitude(latitudeLondon)
                                                    .longitude(longitudeLondon).build())
                                          .find());

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsCloseToAGivenPointWithinARadiusOfMeters() {
        assumeMinServerVersion(2.6);
        // given
        AllTheThings sevilla = new AllTheThings("Spain", geometryCollection(multiPoint(point(37.40759155713022, -5.964911067858338),
                                                                                       point(37.40341208875179, -5.9643941558897495),
                                                                                       point(37.40297396667302, -5.970452763140202)),
                                                                            polygon(point(37.40759155713022, -5.964911067858338),
                                                                                    point(37.40341208875179, -5.9643941558897495),
                                                                                    point(37.40297396667302, -5.970452763140202),
                                                                                    point(37.40759155713022, -5.964911067858338)),
                                                                            polygon(point(37.38744598813355, -6.001141928136349),
                                                                                    point(37.385990973562, -6.002588979899883),
                                                                                    point(37.386126928031445, -6.002463921904564),
                                                                                    point(37.38744598813355, -6.001141928136349))));
        getDs().save(sevilla);

        // insert something that's not a geocollection
        Regions usa = new Regions("US", multiPolygon(polygon(point(40.75981395319104, -73.98302106186748),
                                                             point(40.7636824529618, -73.98049869574606),
                                                             point(40.76962974853814, -73.97964206524193),
                                                             point(40.75981395319104, -73.98302106186748)),
                                                     polygon(point(28.326568258926272, -81.60542246885598),
                                                             point(28.327541397884488, -81.6022228449583),
                                                             point(28.32950334995985, -81.60564735531807),
                                                             point(28.326568258926272, -81.60542246885598))));
        getDs().save(usa);

        AllTheThings london = new AllTheThings("London", geometryCollection(point(53.4722454, -2.2235922),
                                                                            lineString(point(51.507780365645885, -0.21786745637655258),
                                                                                       point(51.50802478194237, -0.21474729292094707),
                                                                                       point(51.5086863655597, -0.20895397290587425)),
                                                                            polygon(point(51.498216362670064, 0.0074849557131528854),
                                                                                    point(51.49176875129342, 0.01821178011596203),
                                                                                    point(51.492886897176504, 0.05523204803466797),
                                                                                    point(51.49393044412136, 0.06663135252892971),
                                                                                    point(51.498216362670064, 0.0074849557131528854))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<AllTheThings> list = toList(getDs().find(AllTheThings.class)
                                                .field("everything")
                                                .near(pointBuilder()
                                                          .latitude(37.3753707)
                                                          .longitude(-5.9550583).build(), 20000)
                                                .find());

        // then
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsOrderedByDistanceFromAGivenPoint() {
        assumeMinServerVersion(2.6);
        // given
        AllTheThings sevilla = new AllTheThings("Spain", geometryCollection(multiPoint(point(37.40759155713022, -5.964911067858338),
                                                                                       point(37.40341208875179, -5.9643941558897495),
                                                                                       point(37.40297396667302, -5.970452763140202)),
                                                                            polygon(point(37.40759155713022, -5.964911067858338),
                                                                                    point(37.40341208875179, -5.9643941558897495),
                                                                                    point(37.40297396667302, -5.970452763140202),
                                                                                    point(37.40759155713022, -5.964911067858338)),
                                                                            polygon(point(37.38744598813355, -6.001141928136349),
                                                                                    point(37.385990973562, -6.002588979899883),
                                                                                    point(37.386126928031445, -6.002463921904564),
                                                                                    point(37.38744598813355, -6.001141928136349))));
        getDs().save(sevilla);

        // insert something that's not a geocollection
        Regions usa = new Regions("US", multiPolygon(polygon(point(40.75981395319104, -73.98302106186748),
                                                             point(40.7636824529618, -73.98049869574606),
                                                             point(40.76962974853814, -73.97964206524193),
                                                             point(40.75981395319104, -73.98302106186748)),
                                                     polygon(point(28.326568258926272, -81.60542246885598),
                                                             point(28.327541397884488, -81.6022228449583),
                                                             point(28.32950334995985, -81.60564735531807),
                                                             point(28.326568258926272, -81.60542246885598))));
        getDs().save(usa);

        AllTheThings london = new AllTheThings("London", geometryCollection(point(53.4722454, -2.2235922),
                                                                            lineString(point(51.507780365645885, -0.21786745637655258),
                                                                                       point(51.50802478194237, -0.21474729292094707),
                                                                                       point(51.5086863655597, -0.20895397290587425)),
                                                                            polygon(point(51.498216362670064, 0.0074849557131528854),
                                                                                    point(51.49176875129342, 0.01821178011596203),
                                                                                    point(51.492886897176504, 0.05523204803466797),
                                                                                    point(51.49393044412136, 0.06663135252892971),
                                                                                    point(51.498216362670064, 0.0074849557131528854))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<AllTheThings> resultsOrderedByDistanceFromLondon = toList(getDs().find(AllTheThings.class)
                                                                              .field("everything")
                                                                              .near(pointBuilder()
                                                                                        .latitude(51.5286416)
                                                                                        .longitude(-0.1015987).build())
                                                                              .find());

        // then
        assertThat(resultsOrderedByDistanceFromLondon.size(), is(2));
        assertThat(resultsOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(resultsOrderedByDistanceFromLondon.get(1), is(sevilla));
    }

    @Test
    public void shouldFindRegionsCloseToAGivenPointWithinARadiusOfMeters() {
        assumeMinServerVersion(2.6);
        // given
        Regions sevilla = new Regions("Spain", multiPolygon(polygon(point(37.40759155713022, -5.964911067858338),
                                                                    point(37.40341208875179, -5.9643941558897495),
                                                                    point(37.40297396667302, -5.970452763140202),
                                                                    point(37.40759155713022, -5.964911067858338)),
                                                            polygon(point(37.38744598813355, -6.001141928136349),
                                                                    point(37.385990973562, -6.002588979899883),
                                                                    point(37.386126928031445, -6.002463921904564),
                                                                    point(37.38744598813355, -6.001141928136349))));
        getDs().save(sevilla);

        Regions usa = new Regions("US", multiPolygon(polygon(point(40.75981395319104, -73.98302106186748),
                                                             point(40.7636824529618, -73.98049869574606),
                                                             point(40.76962974853814, -73.97964206524193),
                                                             point(40.75981395319104, -73.98302106186748)),
                                                     polygon(point(28.326568258926272, -81.60542246885598),
                                                             point(28.327541397884488, -81.6022228449583),
                                                             point(28.32950334995985, -81.60564735531807),
                                                             point(28.326568258926272, -81.60542246885598))));
        getDs().save(usa);

        Regions london = new Regions("London", multiPolygon(polygon(point(51.507780365645885, -0.21786745637655258),
                                                                    point(51.50802478194237, -0.21474729292094707),
                                                                    point(51.5086863655597, -0.20895397290587425),
                                                                    point(51.507780365645885, -0.21786745637655258)),
                                                            polygon(point(51.498216362670064, 0.0074849557131528854),
                                                                    point(51.49176875129342, 0.01821178011596203),
                                                                    point(51.492886897176504, 0.05523204803466797),
                                                                    point(51.49393044412136, 0.06663135252892971),
                                                                    point(51.498216362670064, 0.0074849557131528854))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Regions> regionsOrderedByDistanceFromLondon = toList(getDs().find(Regions.class)
                                                                         .field("regions")
                                                                         .near(pointBuilder()
                                                                                   .latitude(51.5286416)
                                                                                   .longitude(-0.1015987).build(), 20000)
                                                                         .find());

        // then
        assertThat(regionsOrderedByDistanceFromLondon.size(), is(1));
        assertThat(regionsOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindRegionsOrderedByDistanceFromAGivenPoint() {
        assumeMinServerVersion(2.6);
        // given
        Regions sevilla = new Regions("Spain", multiPolygon(polygon(point(37.40759155713022, -5.964911067858338),
                                                                    point(37.40341208875179, -5.9643941558897495),
                                                                    point(37.40297396667302, -5.970452763140202),
                                                                    point(37.40759155713022, -5.964911067858338)),
                                                            polygon(point(37.38744598813355, -6.001141928136349),
                                                                    point(37.385990973562, -6.002588979899883),
                                                                    point(37.386126928031445, -6.002463921904564),
                                                                    point(37.38744598813355, -6.001141928136349))));
        getDs().save(sevilla);

        Regions usa = new Regions("US", multiPolygon(polygon(point(40.75981395319104, -73.98302106186748),
                                                             point(40.7636824529618, -73.98049869574606),
                                                             point(40.76962974853814, -73.97964206524193),
                                                             point(40.75981395319104, -73.98302106186748)),
                                                     polygon(point(28.326568258926272, -81.60542246885598),
                                                             point(28.327541397884488, -81.6022228449583),
                                                             point(28.32950334995985, -81.60564735531807),
                                                             point(28.326568258926272, -81.60542246885598))));
        getDs().save(usa);

        Regions london = new Regions("London", multiPolygon(polygon(point(51.507780365645885, -0.21786745637655258),
                                                                    point(51.50802478194237, -0.21474729292094707),
                                                                    point(51.5086863655597, -0.20895397290587425),
                                                                    point(51.507780365645885, -0.21786745637655258)),
                                                            polygon(point(51.498216362670064, 0.0074849557131528854),
                                                                    point(51.49176875129342, 0.01821178011596203),
                                                                    point(51.492886897176504, 0.05523204803466797),
                                                                    point(51.49393044412136, 0.06663135252892971),
                                                                    point(51.498216362670064, 0.0074849557131528854))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Regions> regionsOrderedByDistanceFromLondon = toList(getDs().find(Regions.class)
                                                                         .field("regions")
                                                                         .near(pointBuilder()
                                                                                   .latitude(51.5286416)
                                                                                   .longitude(-0.1015987).build())
                                                                         .find());

        // then
        assertThat(regionsOrderedByDistanceFromLondon.size(), is(3));
        assertThat(regionsOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(regionsOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(regionsOrderedByDistanceFromLondon.get(2), is(usa));
    }

    @Test
    public void shouldFindRoutesCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        Route sevilla = new Route("Spain", lineString(pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build(),
                                                      pointBuilder().latitude(37.40341208875179).longitude(-5.9643941558897495).build(),
                                                      pointBuilder().latitude(37.40297396667302).longitude(-5.970452763140202).build()));
        getDs().save(sevilla);
        Route newYork = new Route("New York", lineString(pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build(),
                                                         pointBuilder().latitude(40.7636824529618).longitude(-73.98049869574606).build(),
                                                         pointBuilder().latitude(40.76962974853814).longitude(-73.97964206524193).build()));
        getDs().save(newYork);
        Route london = new Route("London", lineString(pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build(),
                                                      pointBuilder().latitude(51.50802478194237).longitude(-0.21474729292094707).build(),
                                                      pointBuilder().latitude(51.5086863655597).longitude(-0.20895397290587425).build()));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Route> routesOrderedByDistanceFromLondon = toList(getDs().find(Route.class)
                                                                      .field("route")
                                                                      .near(pointBuilder()
                                                                                .latitude(51.5286416)
                                                                                .longitude(-0.1015987).build(), 20000)
                                                                      .find());

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(1));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindRoutesOrderedByDistanceFromAGivenPoint() {
        // given
        Route sevilla = new Route("Spain", lineString(pointBuilder().latitude(37.40759155713022).longitude(-5.964911067858338).build(),
                                                      pointBuilder().latitude(37.40341208875179).longitude(-5.9643941558897495).build(),
                                                      pointBuilder().latitude(37.40297396667302).longitude(-5.970452763140202).build()));
        getDs().save(sevilla);
        Route newYork = new Route("New York", lineString(pointBuilder().latitude(40.75981395319104).longitude(-73.98302106186748).build(),
                                                         pointBuilder().latitude(40.7636824529618).longitude(-73.98049869574606).build(),
                                                         pointBuilder().latitude(40.76962974853814).longitude(-73.97964206524193).build()));
        getDs().save(newYork);
        Route london = new Route("London", lineString(pointBuilder().latitude(51.507780365645885).longitude(-0.21786745637655258).build(),
                                                      pointBuilder().latitude(51.50802478194237).longitude(-0.21474729292094707).build(),
                                                      pointBuilder().latitude(51.5086863655597).longitude(-0.20895397290587425).build()));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Route> routesOrderedByDistanceFromLondon = toList(getDs().find(Route.class)
                                                                      .field("route")
                                                                      .near(pointBuilder()
                                                                                .latitude(51.5286416)
                                                                                .longitude(-0.1015987).build())
                                                                      .find());

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(3));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(routesOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(routesOrderedByDistanceFromLondon.get(2), is(newYork));
    }

}
