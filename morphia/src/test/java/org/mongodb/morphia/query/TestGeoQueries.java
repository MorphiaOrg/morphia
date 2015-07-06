package org.mongodb.morphia.query;


import com.mongodb.MongoException;
import com.mongodb.MongoInternalException;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.utils.IndexDirection;

import static org.mongodb.morphia.query.Shape.Point;


public class TestGeoQueries extends TestBase {
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(Place.class);
    }

    @Test
    public void testGeoWithinBox() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(2, 2)))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinOutsideBox() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(.4, .5)))
                                   .get();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinPolygon() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{0, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.polygon(new Point(0, 0), new Point(0, 5), new Point(2, 3), new Point(2, 0)))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinPolygon2() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{10, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.polygon(new Point(0, 0), new Point(0, 5), new Point(2, 3), new Point(2, 0)))
                                   .get();
        Assert.assertNull(found);
    }

    @Test
    public void testGeoWithinRadius() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0, 1), 1.1))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadius2() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0.5, 0.5), 0.77))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testGeoWithinRadiusSphere() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(0, 1), 1))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNear() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .near(0, 0)
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNearMaxDistance() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .near(0, 0, 1.5)
                                   .get();
        Assert.assertNotNull(found);
        final Place notFound = getDs().find(Place.class)
                                      .field("loc")
                                      .near(0, 0, 1)
                                      .get();
        Assert.assertNull(notFound);
    }

    @Test
    public void testNearNoIndex() throws Exception {
        checkMinServerVersion(2.4);
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        Place found = null;
        try {
            found = getDs().find(Place.class)
                           .field("loc")
                           .near(0, 0)
                           .get();
            Assert.assertFalse(true);
        } catch (MongoInternalException e) {
            Assert.assertNull(found);
        } catch (MongoException e) {
            Assert.assertNull(found);
        }
    }

    @Test
    public void testWithinBox() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(2, 2)))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinOutsideBox() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.box(new Point(0, 0), new Point(.4, .5)))
                                   .get();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinOutsideRadius() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(2, 2), .4))
                                   .get();
        Assert.assertNull(found);
    }

    @Test
    public void testWithinRadius() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0, 1), 1.1))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadius2() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.center(new Point(0.5, 0.5), 0.77))
                                   .get();
        Assert.assertNotNull(found);
    }

    @Test
    public void testWithinRadiusSphere() throws Exception {
        checkMinServerVersion(2.4);
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(0, 1), 1))
                                   .get();
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

        public Place(final String name, final double[] loc) {
            this.name = name;
            this.loc = loc;
        }
    }
}
