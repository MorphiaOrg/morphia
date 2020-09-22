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
import dev.morphia.geo.model.AllTheThings;
import dev.morphia.geo.model.Area;
import dev.morphia.geo.model.City;
import dev.morphia.geo.model.Regions;
import dev.morphia.geo.model.Route;
import org.junit.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.near;
import static dev.morphia.query.experimental.filters.Filters.nearSphere;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GeoNearQueriesTest extends TestBase {
    @Test
    public void shouldFindAreasCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        Area sevilla = new Area("Spain", new Polygon(asList(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202),
            new Position(37.40759155713022, -5.964911067858338))));
        getDs().save(sevilla);
        Area newYork = new Area("New York", new Polygon(asList(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193),
            new Position(40.75981395319104, -73.98302106186748))));
        getDs().save(newYork);
        Area london = new Area("London", new Polygon(asList(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425),
            new Position(51.507780365645885, -0.21786745637655258))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = getDs().find(Area.class)
                                                              .filter(near("area", new Point(new Position(51.5286416, -0.1015987)))
                                                                          .maxDistance(20000.0)).iterator()
                                                              .toList();

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(1));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
    }

    @Test
    public void shouldFindAreasOrderedByDistanceFromAGivenPoint() {
        // given
        Area sevilla = new Area("Spain", new Polygon(asList(
            new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202),
            new Position(37.40759155713022, -5.964911067858338))));
        getDs().save(sevilla);
        Area newYork = new Area("New York", new Polygon(asList(
            new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193),
            new Position(40.75981395319104, -73.98302106186748))));
        getDs().save(newYork);
        Area london = new Area("London", new Polygon(asList(
            new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425),
            new Position(51.507780365645885, -0.21786745637655258))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Area> routesOrderedByDistanceFromLondon = getDs().find(Area.class)
                                                              .filter(near("area", new Point(new Position(51.5286416, -0.1015987))))
                                                              .iterator()
                                                              .toList();

        // then
        assertThat(routesOrderedByDistanceFromLondon.size(), is(3));
        assertThat(routesOrderedByDistanceFromLondon.get(0), is(london));
        assertThat(routesOrderedByDistanceFromLondon.get(1), is(sevilla));
        assertThat(routesOrderedByDistanceFromLondon.get(2), is(newYork));
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
                                   .filter(near("location", new Point(new Position(latitudeLondon, longitudeLondon)))).iterator()
                                   .toList();

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));

        cities = getDs().find(City.class)
                        .filter(nearSphere("location", new Point(new Position(latitudeLondon, longitudeLondon)))).iterator()
                        .toList();

        assertThat(cities.size(), is(3));
        assertThat(cities.get(0), is(london));
        assertThat(cities.get(1), is(manchester));
        assertThat(cities.get(2), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsCloseToAGivenPointWithinARadiusOfMeters() {
        // given
        AllTheThings sevilla = new AllTheThings("Spain", new GeometryCollection(asList(
            new MultiPoint(asList(
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
            new Point(new Position(53.4722454, -2.2235922)),
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
        List<AllTheThings> list = getDs().find(AllTheThings.class)
                                         .filter(near("everything", new Point(new Position(37.3753707, -5.9550583)))
                                                     .maxDistance(20000.0)).iterator()
                                         .toList();

        // then
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(sevilla));
    }

    @Test
    public void shouldFindGeometryCollectionsOrderedByDistanceFromAGivenPoint() {
        // given
        AllTheThings sevilla = new AllTheThings("Spain", new GeometryCollection(asList(
            new MultiPoint(asList(
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
            new Point(new Position(53.4722454, -2.2235922)),
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
        List<AllTheThings> results = getDs().find(AllTheThings.class)
                                            .filter(near("everything", new Point(new Position(51.5286416, -0.1015987)))).iterator()
                                            .toList();

        // then
        assertThat(results.size(), is(2));
        assertThat(results.get(0), is(london));
        assertThat(results.get(1), is(sevilla));
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
                                     .filter(near("location", searchPoint).maxDistance(200000.0)).iterator().toList();

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        cities = datastore.find(City.class)
                          .filter(near("location", searchPoint).maxDistance(200000D)).iterator().toList();

        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));

        assertThat(datastore.find(City.class)
                            .filter(near("location", searchPoint)
                                        .maxDistance(200000D)
                                        .minDistance(195000D)).iterator().toList().size(), is(0));

        assertThat(datastore.find(City.class)
                            .filter(nearSphere("location", searchPoint)
                                        .maxDistance(200000D)
                                        .minDistance(195000D)).iterator().toList().size(), is(0));
    }

    @Test
    public void shouldFindRegionsCloseToAGivenPointWithinARadiusOfMeters() {
        // given
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
        List<Regions> regions = getDs().find(Regions.class)
                                       .filter(near("regions", new Point(new Position(51.5286416, -0.1015987)))
                                                   .maxDistance(20000.0)).iterator()
                                       .toList();

        // then
        assertThat(regions.size(), is(1));
        assertThat(regions.get(0), is(london));
    }

    @Test
    public void shouldFindRegionsOrderedByDistanceFromAGivenPoint() {
        // given
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
        List<Regions> regions = getDs().find(Regions.class)
                                       .filter(near("regions", new Point(new Position(51.5286416, -0.1015987)))).iterator()
                                       .toList();

        // then
        assertThat(regions.size(), is(3));
        assertThat(regions.get(0), is(london));
        assertThat(regions.get(1), is(sevilla));
        assertThat(regions.get(2), is(usa));
    }

    @Test
    public void shouldFindRoutesCloseToAGivenPointWithinARadiusOfMeters() {
        // given
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
        getDs().ensureIndexes();

        // when
        List<Route> routes = getDs().find(Route.class)
                                    .filter(near("route", new Point(new Position(51.5286416, -0.1015987)))
                                                .maxDistance(20000.0)).iterator()
                                    .toList();

        // then
        assertThat(routes.size(), is(1));
        assertThat(routes.get(0), is(london));
    }

    @Test
    public void shouldFindRoutesOrderedByDistanceFromAGivenPoint() {
        // given
        Route sevilla = new Route("Spain", new LineString(asList(new Position(37.40759155713022, -5.964911067858338),
            new Position(37.40341208875179, -5.9643941558897495),
            new Position(37.40297396667302, -5.970452763140202))));
        getDs().save(sevilla);
        Route newYork = new Route("New York", new LineString(asList(new Position(40.75981395319104, -73.98302106186748),
            new Position(40.7636824529618, -73.98049869574606),
            new Position(40.76962974853814, -73.97964206524193))));
        getDs().save(newYork);
        Route london = new Route("London", new LineString(asList(new Position(51.507780365645885, -0.21786745637655258),
            new Position(51.50802478194237, -0.21474729292094707),
            new Position(51.5086863655597, -0.20895397290587425))));
        getDs().save(london);
        getDs().ensureIndexes();

        // when
        List<Route> routes = getDs().find(Route.class)
                                    .filter(near("route", new Point(new Position(51.5286416, -0.1015987)))).iterator()
                                    .toList();

        // then
        assertThat(routes.size(), is(3));
        assertThat(routes.get(0), is(london));
        assertThat(routes.get(1), is(sevilla));
        assertThat(routes.get(2), is(newYork));
    }

}
