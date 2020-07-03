package dev.morphia.query;

import dev.morphia.TestBase;
import dev.morphia.geo.AllTheThings;
import dev.morphia.geo.Area;
import dev.morphia.geo.City;
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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoIntersectsQueriesWithPolygonTest extends TestBase {
    @Override
    @Before
    public void setUp() {
        // this whole test class is designed for "modern" geo queries
        assumeMinServerVersion(2.4);
        super.setUp();
    }

    @Test
    public void shouldFindAPointThatLiesInAQueryPolygon() {
        // given
        City manchester = new City("Manchester", point(53.4722454, -2.2235922));
        getDs().save(manchester);
        City london = new City("London", point(51.5286416, -0.1015987));
        getDs().save(london);
        City sevilla = new City("Sevilla", point(37.4057731, -5.966287));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        // when
        List<City> matchingCity = toList(getDs().find(City.class)
                                         .field("location")
                                         .intersects(polygon(point(37.40759155713022, -5.964911067858338),
                                                             point(37.40341208875179, -5.9643941558897495),
                                                             point(37.40297396667302, -5.970452763140202),
                                                             point(37.40759155713022, -5.964911067858338)))
                                         .find());

        // then
        assertThat(matchingCity.size(), is(1));
        assertThat(matchingCity.get(0), is(sevilla));
    }

    @Test
    public void shouldFindAreasThatAPolygonIntersects() {
        // given
        Area sevilla = new Area("Spain",
                                polygon(point(37.40759155713022, -5.964911067858338),
                                        point(37.40341208875179, -5.9643941558897495),
                                        point(37.40297396667302, -5.970452763140202),
                                        point(37.40759155713022, -5.964911067858338))
        );
        getDs().save(sevilla);
        Area newYork = new Area("New York",
                                polygon(point(40.75981395319104, -73.98302106186748),
                                        point(40.7636824529618, -73.98049869574606),
                                        point(40.76962974853814, -73.97964206524193),
                                        point(40.75981395319104, -73.98302106186748)));
        getDs().save(newYork);
        Area london = new Area("London",
                               polygon(point(51.507780365645885, -0.21786745637655258),
                                       point(51.50802478194237, -0.21474729292094707),
                                       point(51.5086863655597, -0.20895397290587425),
                                       point(51.507780365645885, -0.21786745637655258)));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> areaContainingPoint = toList(getDs().find(Area.class)
                                                       .field("area")
                                                       .intersects(polygon(point(37.4056048, -5.9666089),
                                                           point(37.404497, -5.9640557),
                                                           point(37.407239, -5.962988),
                                                           point(37.4056048, -5.9666089)))
                                                       .find());

        // then
        assertThat(areaContainingPoint.size(), is(1));
        assertThat(areaContainingPoint.get(0), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsWhereTheGivenPointIntersectsWithOneOfTheEntities() {
        assumeMinServerVersion(2.6);
        // given
        AllTheThings sevilla = new AllTheThings("Spain", geometryCollection(
                                                                               multiPoint(point(37.40759155713022, -5.964911067858338),
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

        AllTheThings london = new AllTheThings("London", geometryCollection(
                                                                               point(53.4722454, -2.2235922),
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
                                                             .intersects(polygon(point(37.4056048, -5.9666089),
                                                                 point(37.404497, -5.9640557),
                                                                 point(37.407239, -5.962988),
                                                                 point(37.4056048, -5.9666089)))
                                                             .find());

        // then
        assertThat(everythingInTheUK.size(), is(1));
        assertThat(everythingInTheUK.get(0), is(sevilla));
    }

    @Test
    public void shouldFindRegionsThatAPolygonCrosses() {
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
        List<Regions> regionsInTheUK = toList(getDs().find(Regions.class)
                                                     .field("regions")
                                                     .intersects(polygon(point(37.4056048, -5.9666089),
                                                         point(37.404497, -5.9640557),
                                                         point(37.407239, -5.962988),
                                                         point(37.4056048, -5.9666089)))
                                                     .find());

        // then
        assertThat(regionsInTheUK.size(), is(1));
        assertThat(regionsInTheUK.get(0), is(sevilla));
    }

    @Test
    public void shouldFindRoutesThatCrossAQueryPolygon() {
        // given
        Route sevilla = new Route("Spain", lineString(point(37.4056048, -5.9666089),
                                                      point(37.404497, -5.9640557)));
        getDs().save(sevilla);
        Route newYork = new Route("New York", lineString(point(40.75981395319104, -73.98302106186748),
                                                         point(40.7636824529618, -73.98049869574606),
                                                         point(40.76962974853814, -73.97964206524193)));
        getDs().save(newYork);
        Route london = new Route("London", lineString(point(51.507780365645885, -0.21786745637655258),
                                                      point(51.50802478194237, -0.21474729292094707),
                                                      point(51.5086863655597, -0.20895397290587425)));
        getDs().save(london);
        Route londonToParis = new Route("London To Paris", lineString(point(51.5286416, -0.1015987),
                                                                      point(48.858859, 2.3470599)));
        getDs().save(londonToParis);
        getDs().ensureIndexes();

        // when
        List<Route> routeContainingPoint = toList(getDs().find(Route.class)
                                                         .field("route")
                                                         .intersects(polygon(point(37.40759155713022, -5.964911067858338),
                                                             point(37.40341208875179, -5.9643941558897495),
                                                             point(37.40297396667302, -5.970452763140202),
                                                             point(37.40759155713022, -5.964911067858338)))
                                                         .find());

        // then
        assertThat(routeContainingPoint.size(), is(1));
        assertThat(routeContainingPoint.get(0), is(sevilla));
    }

}
