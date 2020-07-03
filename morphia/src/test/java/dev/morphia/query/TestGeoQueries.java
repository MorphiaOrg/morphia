package dev.morphia.query;


import com.mongodb.MongoQueryException;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import static dev.morphia.query.Shape.Point;


public class TestGeoQueries extends TestBase {
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(Place.class);
    }

    @Test
    public void testGeoWithinBox() {
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
        assumeMinServerVersion(2.4);
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
