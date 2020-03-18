package dev.morphia.query;


import com.mongodb.MongoQueryException;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.StringJoiner;

import static dev.morphia.query.experimental.filters.Filters.box;
import static dev.morphia.query.experimental.filters.Filters.center;
import static dev.morphia.query.experimental.filters.Filters.centerSphere;
import static dev.morphia.query.experimental.filters.Filters.near;
import static dev.morphia.query.experimental.filters.Filters.polygon;


public class TestGeoQueries extends TestBase {
    @Override
    public void setUp() {
        super.setUp();
        getMapper().map(Place.class);
    }

    @Test
    public void testGeoWithinBox() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc", new Point(new Position(0, 0)), new Point(new Position(2, 2))))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinOutsideBox() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc", new Point(new Position(0, 0)), new Point(new Position(.4, .5))))
                                   .execute(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinPolygon() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{0, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(polygon("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(0, 5)),
                                       new Point(new Position(2, 3)),
                                       new Point(new Position(2, 0))))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinPolygon2() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{10, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(polygon("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(0, 5)),
                                       new Point(new Position(2, 3)),
                                       new Point(new Position(2, 0))))
                                   .execute(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinRadius() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0, 1)), 1.1))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadius2() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0.5, 0.5)), 0.77))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadiusSphere() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(centerSphere("loc", new Point(new Position(0, 1)), 1))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNear() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(near("loc", new Point(new Position(0, 0))))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNearMaxDistance() {
        getDs().getMapper().map(Place.class);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        FindOptions options = new FindOptions()
                                  .logQuery()
                                  .limit(1);
        Query<Place> query = getDs().find(Place.class)
                                    .filter(near("loc", new Point(new Position(1, 1)))
                                                .maxDistance(2.0));
        Place found = query.execute(options).tryNext();
        Assert.assertNotNull(getDs().getLoggedQuery(options), found);

        final Place notFound = getDs().find(Place.class)
                                      .filter(near("loc", new Point(new Position(0, 0)))
                                                  .maxDistance(1.0))
                                      .execute(options)
                                      .tryNext();
        Assert.assertNull(getDs().getLoggedQuery(options), notFound);
    }

    @Test(expected = MongoQueryException.class)
    public void testNearNoIndex() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        Place found = getDs().find(Place.class)
                             .filter(near("loc", new Point(new Position(0, 0))))
                             .execute(new FindOptions().limit(1))
                             .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinBox() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(2, 2))))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinOutsideBox() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(box("loc",
                                       new Point(new Position(0, 0)),
                                       new Point(new Position(.4, .5))))
                                   .execute(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinOutsideRadius() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(2, 2)), 0.4))
                                   .execute(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinRadius() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0, 1)), 1.1))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadius2() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(center("loc", new Point(new Position(0.5, 0.5)), 0.77))
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadiusSphere() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .filter(centerSphere("loc", new Point(new Position(0, 1)), 1))
                                   .execute(new FindOptions().limit(1))
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

        Place(final String name, final double[] loc) {
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
