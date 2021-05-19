package dev.morphia.test.geo;


import com.mongodb.MongoQueryException;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.TestBase;
import dev.morphia.utils.IndexDirection;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.StringJoiner;

import static dev.morphia.query.experimental.filters.Filters.box;
import static dev.morphia.query.experimental.filters.Filters.center;
import static dev.morphia.query.experimental.filters.Filters.centerSphere;
import static dev.morphia.query.experimental.filters.Filters.near;
import static dev.morphia.query.experimental.filters.Filters.polygon;


public class TestGeoQueries extends TestBase {
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
        Place found = query.iterator(options).tryNext();
        Assert.assertNotNull(found, getDs().getLoggedQuery(options));

        final Place notFound = getDs().find(Place.class)
                                      .filter(near("loc", new Point(new Position(0, 0)))
                                                  .maxDistance(1.0)).iterator(options)
                                      .tryNext();
        Assert.assertNull(notFound, getDs().getLoggedQuery(options));
    }

    @Test(expectedExceptions = MongoQueryException.class)
    public void testNearNoIndex() {
        getMapper().getCollection(Place.class).drop();
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
