package dev.morphia.query;

import dev.morphia.TestBase;
import dev.morphia.geo.AllTheThings;
import dev.morphia.geo.Area;
import dev.morphia.geo.City;
import dev.morphia.geo.MultiPolygon;
import dev.morphia.geo.Polygon;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class GeoWithinQueriesWithMultiPolygonTest extends TestBase {
    private final Polygon uk = polygon(point(49.78, -10.5),
                                       point(49.78, 1.78),
                                       point(59, 1.78),
                                       point(59, -10.5),
                                       point(49.78, -10.5));
    private final Polygon spain = polygon(point(43.40, -10.24),
                                          point(43.40, 3.19),
                                          point(35.45, 3.19),
                                          point(35.45, -10.24),
                                          point(43.40, -10.24));

    @Override
    @Before
    public void setUp() {
        // Multi-polygon is server 2.6 and onwards
        assumeMinServerVersion(2.6);
        super.setUp();
    }

    @Test
    public void shouldFindAreasCompletelyWithinRequiredEuropeanCountries() {
        // given
        MultiPolygon requiredEuropeanCountries = multiPolygon(uk, spain);
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
        Area ukAndSomeOfEurope = new Area("Europe", polygon(
                                                               pointBuilder().latitude(58.0).longitude(-10.0).build(),
                                                               pointBuilder().latitude(58.0).longitude(3).build(),
                                                               pointBuilder().latitude(48.858859).longitude(3).build(),
                                                               pointBuilder().latitude(48.858859).longitude(-10).build(),
                                                               pointBuilder().latitude(58.0).longitude(-10.0).build()));
        getDs().save(ukAndSomeOfEurope);
        getDs().ensureIndexes();

        // when
        List<Area> areasInTheUK = toList(getDs().find(Area.class)
                                         .field("area")
                                         .within(requiredEuropeanCountries)
                                         .find());

        // then
        assertThat(areasInTheUK.size(), is(2));
        assertThat(areasInTheUK, containsInAnyOrder(london, sevilla));
    }

    @Test
    public void shouldFindCitiesInEurope() {
        // given
        MultiPolygon europeanCountries = multiPolygon(uk, spain);

        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City london = new City("London", point(51.5286416, -0.1015987));
        getDs().save(london);
        City sevilla = new City("Sevilla", point(37.3753708, -5.9550582));
        getDs().save(sevilla);
        City newYork = new City("New York", point(40.75981395319104, -73.98302106186748));
        getDs().save(newYork);

        getDs().ensureIndexes();

        // when
        List<City> citiesInTheUK;
        citiesInTheUK = toList(getDs().find(City.class)
                               .field("location")
                               .within(europeanCountries)
                               .find());

        // then
        assertThat(citiesInTheUK.size(), is(3));
        assertThat(citiesInTheUK, containsInAnyOrder(london, manchester,
                                                     sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsWithinEurope() {
        // given
        MultiPolygon europeanCountries = multiPolygon(uk, spain);

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
        List<AllTheThings> everythingInTheUK = toList(getDs().find(AllTheThings.class)
                                                      .field("everything")
                                                      .within(europeanCountries)
                                                      .find());

        // then
        assertThat(everythingInTheUK.size(), is(2));
        assertThat(everythingInTheUK, containsInAnyOrder(london, sevilla));
    }

    @Test
    public void shouldFindRegionsWithinEurope() {
        // given
        MultiPolygon europeanCountries = multiPolygon(uk, spain);
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
        List<Regions> regionsInTheUK = toList(getDs().find(Regions.class)
                                              .field("regions")
                                              .within(europeanCountries)
                                              .find());

        // then
        assertThat(regionsInTheUK.size(), is(2));
        assertThat(regionsInTheUK, containsInAnyOrder(sevilla, london));
    }

    @Test
    public void shouldFindRoutesCompletelyWithinRequiredEuropeCountries() {
        // given
        MultiPolygon requiredEuropeanCountries = multiPolygon(uk, spain);
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
        Route londonToParis = new Route("London To Paris", lineString(pointBuilder().latitude(51.5286416).longitude(-0.1015987).build(),
                                                                      pointBuilder().latitude(48.858859).longitude(2.3470599).build()));
        getDs().save(londonToParis);
        getDs().ensureIndexes();

        // when
        List<Route> routesInTheUK = toList(getDs().find(Route.class)
                                           .field("route")
                                           .within(requiredEuropeanCountries)
                                           .find());

        // then
        assertThat(routesInTheUK.size(), is(2));
        assertThat(routesInTheUK, containsInAnyOrder(london, sevilla));
    }

}
