package xyz.morphia.query;


import com.mongodb.MongoQueryException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Indexed;
import xyz.morphia.utils.IndexDirection;

import static xyz.morphia.query.Shape.Point;


public class TestGeoQueries extends TestBase {
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(Place.class);
    }

    @Test
    public void testGeoWithinBox() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(2, 2)))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinOutsideBox() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(.4, .5)))
                                   .find(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinPolygon() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{0, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.polygon(new Point(0, 0), new Point(0, 5), new Point(2, 3), new Point(2, 0)))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinPolygon2() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{10, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.polygon(new Point(0, 0), new Point(0, 5), new Point(2, 3), new Point(2, 0)))
                                   .find(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinRadius() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0, 1), 1.1))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadius2() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0.5, 0.5), 0.77))

                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadiusSphere() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(0, 1), 1))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNear() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .near(0, 0)
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNearMaxDistance() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .near(0, 0, 1.5)
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
        final Place notFound = getDs().find(Place.class)
                                      .field("loc")
                                      .near(0, 0, 1)
                                      .find(new FindOptions().limit(1))
                                      .tryNext();
        Assert.assertNull(notFound);
    }

    @Test(expected = MongoQueryException.class)
    public void testNearNoIndex() {
        checkMinServerVersion(2.4);
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        Place found = getDs().find(Place.class)
                             .field("loc")
                             .near(0, 0)
                             .find(new FindOptions().limit(1))
                             .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinBox() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(2, 2)))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinOutsideBox() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(.4, .5)))
                                   .find(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinOutsideRadius() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(2, 2), .4))
                                   .find(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinRadius() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0, 1), 1.1))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadius2() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0.5, 0.5), 0.77))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadiusSphere() {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(0, 1), 1))
                                   .find(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Entity
    private static class Place {
        @Id
        private ObjectId id;
        private String name;
        @Indexed(IndexDirection.GEO2D)
        private double[] loc;

        private Place() {
        }

        Place(final String name, final double[] loc) {
            this.name = name;
            this.loc = loc;
        }
    }
}
