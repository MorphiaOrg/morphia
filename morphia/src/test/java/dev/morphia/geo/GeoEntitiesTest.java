package dev.morphia.geo;

import com.mongodb.client.model.geojson.GeometryCollection;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.MultiLineString;
import com.mongodb.client.model.geojson.MultiPoint;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.geo.model.Area;
import dev.morphia.geo.model.City;
import dev.morphia.geo.model.Regions;
import dev.morphia.geo.model.Route;
import dev.morphia.geo.model.Stores;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test driving features for Issue 643 - add support for saving entities with GeoJSON.
 */
public class GeoEntitiesTest extends TestBase {

    @Test
    public void shouldRetrieveGeoCollectionType() {
        String name = "What, everything?";
        LineString lineString = new LineString(asList(
            new Position(1.0, 2.0),
            new Position(3.0, 5.0),
            new Position(19.0, 13.0)));

        Polygon polygonWithHoles = new Polygon(
            new PolygonCoordinates(asList(new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),

                asList(new Position(1.5, 2.0),
                    new Position(1.9, 2.0),
                    new Position(1.9, 1.8),
                    new Position(1.5, 2.0)),

                asList(new Position(2.2, 2.1),
                    new Position(2.4, 1.9),
                    new Position(2.4, 1.7),
                    new Position(2.1, 1.8),
                    new Position(2.2, 2.1))));

        MultiPoint multiPoint = new MultiPoint(asList(
            new Position(1.0, 2.0),
            new Position(3.0, 5.0),
            new Position(19.0, 13.0)));

        MultiLineString multiLineString = new MultiLineString(asList(
            asList(new Position(1, 2), new Position(3, 5), new Position(19, 13)),

            asList(new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0))));

        MultiPolygon multiPolygon = new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)))));

        GeometryCollection geometryCollection = new GeometryCollection(asList(lineString, polygonWithHoles, multiPoint, multiLineString,
            multiPolygon));

        AllTheThings allTheThings = new AllTheThings(name, geometryCollection);
        getDs().save(allTheThings);

        // when
        AllTheThings found = getDs()
                                 .find(AllTheThings.class)
                                 .filter(eq("name", name))
                                 .execute(new FindOptions().limit(1))
                                 .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(allTheThings));
    }

    @Test
    public void shouldRetrieveGeoJsonLineString() {
        // given
        Route route = new Route("My Route", new LineString(asList(
            new Position(1, 2),
            new Position(3, 5),
            new Position(19, 13))));
        getDs().save(route);

        // when
        Route found = getDs().find(Route.class)
                             .filter(eq("name", "My Route"))
                             .execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(route));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiLineString() {
        // given
        String name = "Many Paths";
        Paths paths = new Paths(name, new MultiLineString(
            asList(
                asList(
                    new Position(1, 2),
                    new Position(3, 5),
                    new Position(19, 13)),

                asList(
                    new Position(1.5, 2.0),
                    new Position(1.9, 2.0),
                    new Position(1.9, 1.8),
                    new Position(1.5, 2.0)))));
        
        getDs().save(paths);

        // when
        Paths found = getDs().find(Paths.class).filter(eq("name", name)).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(paths));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPoint() {
        // given
        String name = "My stores";
        Stores stores = new Stores(name, new MultiPoint(asList(
            new Position(1, 2),
            new Position(3, 5),
            new Position(19, 13))));
        getDs().save(stores);

        // when
        Stores found = getDs().find(Stores.class).filter(eq("name", name)).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(stores));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPolygon() {
        // given
        String name = "All these shapes";
        PolygonCoordinates polygonWithHoles = new PolygonCoordinates(
            asList(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),

            asList(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)),
            asList(
                new Position(2.2, 2.1),
                new Position(2.4, 1.9),
                new Position(2.4, 1.7),
                new Position(2.1, 1.8),
                new Position(2.2, 2.1)));

        Regions regions = new Regions(name, new MultiPolygon(asList(
            new PolygonCoordinates(asList(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0))),
            polygonWithHoles)));
        getDs().save(regions);

        // when
        Regions found = getDs().find(Regions.class)
                               .filter(eq("name", name))
                               .execute(new FindOptions().limit(1))
                               .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(regions));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiRingPolygon() {
        // given
        String polygonName = "A polygon with holes";
        Polygon polygonWithHoles = new Polygon(
            asList(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),
            asList(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)),
            asList(
                new Position(2.2, 2.1),
                new Position(2.4, 1.9),
                new Position(2.4, 1.7),
                new Position(2.1, 1.8),
                new Position(2.2, 2.1)));

        Area area = new Area(polygonName, polygonWithHoles);
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class)
                            .filter(eq("name", polygonName))
                            .execute(new FindOptions().limit(1))
                            .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }

    @Test
    public void shouldRetrieveGeoJsonPoint() {
        // given
        City city = new City("New City", new Point(new Position(3.0, 7.0)));
        getDs().save(city);

        // when
        City found = getDs().find(City.class)
                            .filter(eq("name", "New City"))
                            .execute(new FindOptions().limit(1))
                            .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(city));
    }

    @Test
    public void shouldRetrieveGeoJsonPolygon() {
        // given
        Area area = new Area("The Area", new Polygon(asList(
            new Position(2.0, 1.1),
            new Position(3.5, 2.3),
            new Position(1.0, 3.7),
            new Position(2.0, 1.1))));
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class)
                            .filter(eq("name", "The Area"))
                            .execute(new FindOptions().limit(1))
                            .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }


    @Entity
    private static final class Paths {
        @Id
        private ObjectId id;
        private String name;
        private MultiLineString paths;

        private Paths() {
        }

        private Paths(final String name, final MultiLineString paths) {
            this.name = name;
            this.paths = paths;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + paths.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Paths paths1 = (Paths) o;

            if (!name.equals(paths1.name)) {
                return false;
            }
            if (!paths.equals(paths1.paths)) {
                return false;
            }

            return true;
        }


        @Override
        public String toString() {
            return "Paths{"
                   + "name='" + name + '\''
                   + ", paths=" + paths
                   + '}';
        }
    }

    @Entity
    private static final class AllTheThings {
        @Id
        private ObjectId id;
        private GeometryCollection everything;
        private String name;

        private AllTheThings() {
        }

        private AllTheThings(final String name, final GeometryCollection everything) {
            this.name = name;
            this.everything = everything;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AllTheThings that = (AllTheThings) o;

            if (!everything.equals(that.everything)) {
                return false;
            }
            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = everything.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AllTheThings{"
                   + "everything=" + everything
                   + ", name='" + name + '\''
                   + '}';
        }
    }
}
