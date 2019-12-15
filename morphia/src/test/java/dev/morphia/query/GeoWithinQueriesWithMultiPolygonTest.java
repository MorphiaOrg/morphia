package dev.morphia.query;

import com.mongodb.client.model.geojson.GeometryCollection;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.MultiPoint;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.geo.model.AllTheThings;
import dev.morphia.geo.model.Area;
import dev.morphia.geo.model.City;
import dev.morphia.geo.model.Regions;
import dev.morphia.geo.model.Route;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class GeoWithinQueriesWithMultiPolygonTest extends TestBase {
    private final PolygonCoordinates uk = new PolygonCoordinates(asList(new Position(49.78, -10.5),
        new Position(49.78, 1.78),
        new Position((double) 59, 1.78),
        new Position((double) 59, -10.5),
        new Position(49.78, -10.5)));
    private final PolygonCoordinates spain = new PolygonCoordinates(asList(new Position(43.40, -10.24),
        new Position(43.40, 3.19),
        new Position(35.45, 3.19),
        new Position(35.45, -10.24),
        new Position(43.40, -10.24)));
    private final MultiPolygon europeanCountries = new MultiPolygon(asList(uk, spain));

    @Override
    @Before
    public void setUp() {
        // Multi-polygon is server 2.6 and onwards
        checkMinServerVersion(2.6);
        super.setUp();
    }

    @Test
    public void shouldFindAreasCompletelyWithinRequiredEuropeanCountries() {
        Area sevilla = new Area("Spain", new Polygon(asList(new Position(37.40759155713022, -5.964911067858338),
                                                 new Position(37.40341208875179,-5.9643941558897495),
                                                 new Position(37.40297396667302,-5.970452763140202),
                                                 new Position(37.40759155713022,-5.964911067858338))));
        getDs().save(sevilla);
        Area newYork = new Area("New York", new Polygon(asList(new Position(40.75981395319104,-73.98302106186748),
                                                    new Position(40.7636824529618,-73.98049869574606),
                                                    new Position(40.76962974853814,-73.97964206524193),
                                                    new Position(40.75981395319104,-73.98302106186748))));
        getDs().save(newYork);
        Area london = new Area("London", new Polygon(asList(new Position(51.507780365645885,-0.21786745637655258),
                                                 new Position(51.50802478194237,-0.21474729292094707),
                                                 new Position(51.5086863655597,-0.20895397290587425),
                                                 new Position(51.507780365645885,-0.21786745637655258))));
        getDs().save(london);
        Area ukAndSomeOfEurope = new Area("Europe", new Polygon(asList(
                                                               new Position(58.0,-10.0),
                                                               new Position(58.0,3),
                                                               new Position(48.858859,3),
                                                               new Position(48.858859,-10),
                                                               new Position(58.0,-10.0))));
        getDs().save(ukAndSomeOfEurope);
        getDs().ensureIndexes();

        // when
        List<Area> areasInTheUK = getDs().find(Area.class)
                                         .field("area")
                                         .within(europeanCountries)
                                         .execute().toList();

        // then
        assertThat(areasInTheUK.size(), is(2));
        assertThat(areasInTheUK, containsInAnyOrder(london, sevilla));
    }

    @Test
    public void shouldFindCitiesInEurope() {
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        City london = new City("London", new Point(new Position(51.5286416, -0.1015987)));
        getDs().save(london);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);
        City newYork = new City("New York", new Point(new Position(40.75981395319104, -73.98302106186748)));
        getDs().save(newYork);

        getDs().ensureIndexes();

        // when
        List<City> citiesInTheUK;
        citiesInTheUK = getDs().find(City.class)
                               .field("location")
                               .within(europeanCountries)
                               .execute().toList();

        // then
        assertThat(citiesInTheUK.size(), is(3));
        assertThat(citiesInTheUK, containsInAnyOrder(london, manchester, sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsWithinEurope() {
        AllTheThings sevilla = new AllTheThings("Spain", new GeometryCollection(asList(new MultiPoint(
            asList(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202))),
            new Polygon(asList(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new Polygon(asList(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        // insert something that's not a geocollection
        Regions usa = new Regions("US", new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(asList(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        AllTheThings london = new AllTheThings("London", new GeometryCollection(asList(
            new LineString(asList(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425))),
            new Polygon(asList(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<AllTheThings> everythingInTheUK = getDs().find(AllTheThings.class)
                                                      .field("everything")
                                                      .within(europeanCountries)
                                                      .execute().toList();

        // then
        assertThat(everythingInTheUK.size(), is(2));
        assertThat(everythingInTheUK, containsInAnyOrder(london, sevilla));
    }

    @Test
    public void shouldFindRegionsWithinEurope() {
        Regions sevilla = new Regions("Spain", new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new PolygonCoordinates(asList(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        Regions usa = new Regions("US", new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(asList(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        Regions london = new Regions("London", new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425),
                new Position(51.507780365645885, -0.21786745637655258))),
            new PolygonCoordinates(asList(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Regions> regionsInTheUK = getDs().find(Regions.class)
                                              .field("regions")
                                              .within(europeanCountries)
                                              .execute()
                                              .toList();

        // then
        assertThat(regionsInTheUK.size(), is(2));
        assertThat(regionsInTheUK, containsInAnyOrder(sevilla, london));
    }

    @Test
    public void shouldFindRoutesCompletelyWithinRequiredEuropeCountries() {
        Route sevilla = new Route("Spain", new LineString(asList(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202))));
        getDs().save(sevilla);
        Route newYork = new Route("New York", new LineString(asList(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193))));
        getDs().save(newYork);
        Route london = new Route("London", new LineString(asList(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425))));
        getDs().save(london);
        Route londonToParis = new Route("London To Paris", new LineString(asList(
            new Position(51.5286416, -0.1015987),
            new Position(48.858859, 2.3470599))));
        getDs().save(londonToParis);
        getDs().ensureIndexes();

        // when
        List<Route> routesInTheUK = getDs().find(Route.class)
                                           .field("route")
                                           .within(europeanCountries)
                                           .execute()
                                           .toList();

        // then
        assertThat(routesInTheUK.size(), is(2));
        assertThat(routesInTheUK, containsInAnyOrder(london, sevilla));
    }

}
