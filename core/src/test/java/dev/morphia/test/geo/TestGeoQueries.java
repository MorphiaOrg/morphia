package dev.morphia.test.geo;


import com.mongodb.MongoQueryException;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;
import dev.morphia.utils.IndexDirection;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static dev.morphia.query.experimental.filters.Filters.box;
import static dev.morphia.query.experimental.filters.Filters.center;
import static dev.morphia.query.experimental.filters.Filters.centerSphere;
import static dev.morphia.query.experimental.filters.Filters.near;
import static dev.morphia.query.experimental.filters.Filters.nearSphere;
import static dev.morphia.query.experimental.filters.Filters.polygon;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class TestGeoQueries extends TestBase {
    @Test
    public void geoIntersects() {
        // given
        City manchester = new City("Manchester", new Point(new Position(53.4722454, -2.2235922)));
        getDs().save(manchester);
        getDs().save(new City("London", new Point(new Position(51.5286416, -0.1015987))));
        City sevilla = getDs().save(new City("Sevilla", new Point(new Position(37.4057731, -5.966287))));

        getDs().ensureIndexes();

        // when
        List<City> matchingCity = getDs().find(City.class)
                                         .filter(Filters.geoIntersects("location", new Polygon(asList(
                                             new Position(37.40759155713022, -5.964911067858338),
                                             new Position(37.40341208875179, -5.9643941558897495),
                                             new Position(37.40297396667302, -5.970452763140202),
                                             new Position(37.40759155713022, -5.964911067858338))))).iterator().toList();

        // then
        assertThat(matchingCity.size(), is(1));
        assertThat(matchingCity.get(0), is(sevilla));
    }

    @Test
    public void maxDistance() {
        // given
        double latitude = 51.5286416;
        double longitude = -0.1015987;
        Datastore datastore = getDs();
        City london = datastore.save(new City("London", new Point(new Position(latitude, longitude))));
        datastore.save(List.of(new City("Manchester", new Point(new Position(53.4722454, -2.2235922))),
            new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)))));

        getDs().ensureIndexes();

        // when
        List<City> cities = datastore.find(City.class)
                                     .filter(near("location", new Point(new Position(latitude, longitude)))
                                                 .maxDistance(200000.0)).iterator()
                                     .toList();

        // then
        assertThat(cities.size(), is(1));
        assertThat(cities.get(0), is(london));
    }

    @BeforeMethod
    public void setUp() {
        getMapper().map(Place.class);
        getDs().ensureIndexes();
    }

    @Test
    public void testGeoWithinBox() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc", new Point(new Position(0, 0)), new Point(new Position(2, 2))))
                                   .iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinOutsideBox() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc", new Point(new Position(0, 0)), new Point(new Position(.4, .5))))
                                   .iterator(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinPolygon() {
        final Place place1 = new Place("place1", new double[]{0, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(polygon("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(0, 5)),
                                       new Point(new Position(2, 3)),
                                       new Point(new Position(2, 0)))).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinPolygon2() {
        final Place place1 = new Place("place1", new double[]{10, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(polygon("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(0, 5)),
                                       new Point(new Position(2, 3)),
                                       new Point(new Position(2, 0)))).iterator(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinRadius() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0, 1)), 1.1)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadius2() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0.5, 0.5)), 0.77)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadiusSphere() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(centerSphere("loc", new Point(new Position(0, 1)), 1)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNear() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(near("loc", new Point(new Position(0, 0)))).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNearMaxDistance() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        FindOptions options = new FindOptions()
            .logQuery()
            .limit(1);
        Query<Place> query = getDs().find(Place.class)
                                    .filter(near("loc", new Point(new Position(1, 1)))
                                        .maxDistance(2.0));
        Assert.assertNotNull(query.iterator(options).tryNext(), query.getLoggedQuery());

        query = getDs().find(Place.class)
                       .filter(near("loc", new Point(new Position(0, 0)))
                           .maxDistance(1.0));
        Assert.assertNull(query.first(options), query.getLoggedQuery());
    }

    @Test(expectedExceptions = MongoQueryException.class)
    public void testNearNoIndex() {
        getDs().getCollection(Place.class).drop();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        Place found = getDs().find(Place.class)
                             .filter(near("loc", new Point(new Position(0, 0)))).iterator(new FindOptions().limit(1))
                             .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinBox() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(2, 2)))).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinOutsideBox() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(.4, .5)))).iterator(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinOutsideRadius() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(2, 2)), 0.4)).iterator(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinRadius() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0, 1)), 1.1)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadius2() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0.5, 0.5)), 0.77)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadiusSphere() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(centerSphere("loc", new Point(new Position(0, 1)), 1)).iterator(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void minDistance() {
        // given
        Datastore datastore = getDs();
        City london = new City("London", new Point(new Position(51.5286416, -0.1015987)));
        datastore.save(london);
        datastore.save(List.of(new City("Manchester", new Point(new Position(53.4722454, -2.2235922))),
            new City("Sevilla", new Point(new Position(37.3753708, -5.9550582)))));

        getDs().ensureIndexes();

        final Point searchPoint = new Point(new Position(50, 0.1278));

        assertThat(datastore.find(City.class)
                            .filter(near("location", searchPoint)
                                        .maxDistance(200000D)
                                        .minDistance(195000D))
                            .iterator().toList().size(), is(0));

        assertThat(datastore.find(City.class)
                            .filter(nearSphere("location", searchPoint)
                                        .maxDistance(200000D)
                                        .minDistance(195000D))
                            .iterator().toList().size(), is(0));
    }

    @Entity
    private static class City {
        @Id
        private ObjectId id;
        @Indexed(IndexDirection.GEO2DSPHERE)
        private Point location;
        private String name;

        //needed for Morphia serialisation
        @SuppressWarnings("unused")
        public City() {
        }

        public City(String name, Point location) {
            this.name = name;
            this.location = location;
        }

        @Override
        public int hashCode() {
            int result = location != null ? location.hashCode() : 0;
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof City)) {
                return false;
            }

            final City city = (City) o;

            if (location != null ? !location.equals(city.location) : city.location != null) {
                return false;
            }
            return name.equals(city.name);

        }

        @Override
        public String toString() {
            return "City{"
                   + "location=" + location
                   + ", name='" + name + '\''
                   + '}';
        }
    }

    @Entity
    private static class Place {
        @Id
        private ObjectId id;
        private String name;
        @Indexed(IndexDirection.GEO2DSPHERE)
        private double[] loc;

        private Place() {
        }

        Place(String name, double[] loc) {
            this.name = name;
            this.loc = loc;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Place.class.getSimpleName() + "[", "]")
                       .add("name='" + name + "'")
                       .add("loc=" + Arrays.toString(loc))
                       .toString();
        }
    }
}
