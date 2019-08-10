package dev.morphia.query;

import com.mongodb.client.model.geojson.GeometryCollection;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.MultiPoint;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.geo.AllTheThings;
import dev.morphia.geo.Area;
import dev.morphia.geo.City;
import dev.morphia.geo.Regions;
import dev.morphia.geo.Route;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
@Ignore("geo needs work")
public class GeoNearQueriesTest extends TestBase {

    @Override
    @Before
    public void setUp() {
        // this whole test class is designed for "modern" geo queries
        checkMinServerVersion(2.4);
        super.setUp();
    }

    @Test
    public void shouldFindAreasCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        Area sevilla = new Area("Spain", new Polygon(of(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202),
            new Position(37.40759155713022, -5.964911067858338))));
        getDs().save(sevilla);
        Area newYork = new Area("New York", new Polygon(of(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193),
            new Position(40.75981395319104, -73.98302106186748))));
        getDs().save(newYork);
        Area london = new Area("London", new Polygon(of(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425),
            new Position(51.507780365645885, -0.21786745637655258))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = getDs().find(Area.class)
                                                              .field("area")
                                                              .near(new Point(new Position(51.5286416, -0.1015987)), 20000.0, null)
                                                              .execute()
                                                              .toList();

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(1));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindAreasOrderedByDistanceFromAGivenPoint() {
        // given
        Area sevilla = new Area("Spain", new Polygon(of(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202),
            new Position(37.40759155713022, -5.964911067858338))));
        getDs().save(sevilla);
        Area newYork = new Area("New York", new Polygon(of(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193),
            new Position(40.75981395319104, -73.98302106186748))));
        getDs().save(newYork);
        Area london = new Area("London", new Polygon(of(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425),
            new Position(51.507780365645885, -0.21786745637655258))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = getDs().find(Area.class)
                                                              .field("area")
                                                              .near(new Point(new Position(51.5286416, -0.1015987)))
                                                              .execute()
                                                              .toList();

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(3));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(routesOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(routesOrderedByDistanceFromLondon.get(2), is(newYork));
    }

    @Test
    public void shouldFindNearAPoint() {
        // given
        Datastore datastore = getDs();
        City london = new City("London", new Point(new Position(51.5286416, -0.1015987)));
        datastore.save(london);
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        datastore.save(manchester);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        datastore.save(sevilla);

        getDs().ensureIndexes();

        final Point searchPoint = new Point(new Position(50, 0.1278));
        List<City> cities = datastore.find(City.class)
                                     .field("location")
                                     .near(searchPoint, 200000.0, null)
                                     .execute().toList();

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        cities = datastore.find(City.class)
                          .field("location")
                          .nearSphere(searchPoint, 200000D, null)
                          .execute().toList();

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        assertThat(datastore.find(City.class)
                            .field("location")
                            .near(searchPoint, 200000D, 195000D)
                            .execute().toList().size(), is(0));

        assertThat(datastore.find(City.class)
                            .field("location")
                            .nearSphere(searchPoint, 200000D, 195000D)
                            .execute().toList().size(), is(0));
    }

    @Test
    public void shouldFindCitiesOrderedByDistance() {
        double latitudeLondon = 51.5286416;
        double longitudeLondon = -0.1015987;
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        City london = new City("London", new Point(new Position(latitudeLondon, longitudeLondon)));
        getDs().save(london);
        City sevilla = new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)));
        getDs().save(sevilla);

        getDs().ensureIndexes();

        List<City> cities = getDs().find(City.class)
                                   .field("location")
                                   .near(new Point(new Position(latitudeLondon, longitudeLondon)))
                                   .execute()
                                   .toList();

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));

        cities = getDs().find(City.class)
                        .field("location")
                        .nearSphere(new Point(new Position(latitudeLondon, longitudeLondon)))
                        .execute()
                        .toList();

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsCloseToAGivenPointWithinARadiusOfMeters() {
        checkMinServerVersion(2.6);
        // given
        AllTheThings sevilla = new AllTheThings("Spain", new GeometryCollection(of(
            new MultiPoint(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202))),
            new Polygon(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new Polygon(of(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        // insert something that's not a geocollection
        Regions usa = new Regions("US", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(of(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        AllTheThings london = new AllTheThings("London", new GeometryCollection(of(
            new Point(new Position(53.4722454, -2.2235922)),
            new LineString(of(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425))),
            new Polygon(of(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<AllTheThings> list = getDs().find(AllTheThings.class)
                                         .field("everything")
                                         .near(new Point(new Position(37.3753707, -5.9550583)), 20000.0, null)
                                         .execute()
                                         .toList();

        // then
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsOrderedByDistanceFromAGivenPoint() {
        checkMinServerVersion(2.6);
        // given
        AllTheThings sevilla = new AllTheThings("Spain", new GeometryCollection(of(
            new MultiPoint(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202))),
            new Polygon(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new Polygon(of(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        // insert something that's not a geocollection
        Regions usa = new Regions("US", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(of(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        AllTheThings london = new AllTheThings("London", new GeometryCollection(of(
            new Point(new Position(53.4722454, -2.2235922)),
            new LineString(of(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425))),
            new Polygon(of(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<AllTheThings> resultsOrderedByDistanceFromLondon = getDs().find(AllTheThings.class)
                                                                       .field("everything")
                                                                       .near(new Point(new Position(51.5286416, -0.1015987)))
                                                                       .execute()
                                                                       .toList();

        // then
        assertThat(resultsOrderedByDistanceFromLondon.size(), is(2));
        assertThat(resultsOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(resultsOrderedByDistanceFromLondon.get(1), is(sevilla));
    }

    @Test
    public void shouldFindRegionsCloseToAGivenPointWithinARadiusOfMeters() {
        checkMinServerVersion(2.6);
        // given
        Regions sevilla = new Regions("Spain", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new PolygonCoordinates(of(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        Regions usa = new Regions("US", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(of(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        Regions london = new Regions("London", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425),
                new Position(51.507780365645885, -0.21786745637655258))),
            new PolygonCoordinates(of(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Regions> regions = getDs().find(Regions.class)
                                       .field("regions")
                                       .near(new Point(new Position(51.5286416, -0.1015987)), 20000.0, null)
                                       .execute()
                                       .toList();

        // then
        assertThat(regions.size(), is(1));
        assertThat(regions.get(0), is(london));
    }

    @Test
    public void shouldFindRegionsOrderedByDistanceFromAGivenPoint() {
        checkMinServerVersion(2.6);
        // given
        Regions sevilla = new Regions("Spain", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(37.40759155713022, -5.964911067858338),
                new Position(37.40341208875179, -5.9643941558897495),
                new Position(37.40297396667302, -5.970452763140202),
                new Position(37.40759155713022, -5.964911067858338))),
            new PolygonCoordinates(of(
                new Position(37.38744598813355, -6.001141928136349),
                new Position(37.385990973562, -6.002588979899883),
                new Position(37.386126928031445, -6.002463921904564),
                new Position(37.38744598813355, -6.001141928136349))))));
        getDs().save(sevilla);

        Regions usa = new Regions("US", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(40.75981395319104, -73.98302106186748),
                new Position(40.7636824529618, -73.98049869574606),
                new Position(40.76962974853814, -73.97964206524193),
                new Position(40.75981395319104, -73.98302106186748))),
            new PolygonCoordinates(of(
                new Position(28.326568258926272, -81.60542246885598),
                new Position(28.327541397884488, -81.6022228449583),
                new Position(28.32950334995985, -81.60564735531807),
                new Position(28.326568258926272, -81.60542246885598))))));
        getDs().save(usa);

        Regions london = new Regions("London", new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(51.507780365645885, -0.21786745637655258),
                new Position(51.50802478194237, -0.21474729292094707),
                new Position(51.5086863655597, -0.20895397290587425),
                new Position(51.507780365645885, -0.21786745637655258))),
            new PolygonCoordinates(of(
                new Position(51.498216362670064, 0.0074849557131528854),
                new Position(51.49176875129342, 0.01821178011596203),
                new Position(51.492886897176504, 0.05523204803466797),
                new Position(51.49393044412136, 0.06663135252892971),
                new Position(51.498216362670064, 0.0074849557131528854))))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Regions> regionsOrderedByDistanceFromLondon = getDs().find(Regions.class)
                                                                  .field("regions")
                                                                  .near(new Point(new Position(51.5286416, -0.1015987)))
                                                                  .execute()
                                                                  .toList();

        // then
        assertThat(regionsOrderedByDistanceFromLondon.size(), is(3));
        assertThat(regionsOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(regionsOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(regionsOrderedByDistanceFromLondon.get(2), is(usa));
    }

    @Test
    public void shouldFindRoutesCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        Route sevilla = new Route("Spain", new LineString(of(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202))));
        getDs().save(sevilla);
        Route newYork = new Route("New York", new LineString(of(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193))));
        getDs().save(newYork);
        Route london = new Route("London", new LineString(of(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Route> routes = getDs().find(Route.class)
                                    .field("route")
                                    .near(new Point(new Position(51.5286416, -0.1015987)), 20000.0, null)
                                    .execute().toList();

        // then
        assertThat(routes.size(), is(1));
        assertThat(routes.get(0), is(london));
    }

    @Test
    public void shouldFindRoutesOrderedByDistanceFromAGivenPoint() {
        // given
        Route sevilla = new Route("Spain", new LineString(of(new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202))));
        getDs().save(sevilla);
        Route newYork = new Route("New York", new LineString(of(new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193))));
        getDs().save(newYork);
        Route london = new Route("London", new LineString(of(new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Route> routes = getDs().find(Route.class)
                                    .field("route")
                                    .near(new Point(new Position(51.5286416, -0.1015987)))
                                    .execute()
                                    .toList();

        // then
        assertThat(routes.size(), is(3));
        assertThat(routes.get(0), is(london));
        assertThat(routes.get(1), is(sevilla));
        assertThat(routes.get(2), is(newYork));
    }

}
