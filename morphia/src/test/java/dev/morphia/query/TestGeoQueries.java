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
                                   .field("loc")
                                   .within(Shape.box(new Point(new Position(0, 0)), new Point(new Position(2, 2))))
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
                                   .field("loc")
                                   .within(Shape.box(new Point(new Position(0, 0)), new Point(new Position(.4, .5))))
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
                                   .field("loc")
                                   .within(Shape.polygon(
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
                                   .field("loc")
                                   .within(Shape.polygon(
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
                                   .field("loc")
                                   .within(Shape.center(new Point(new Position(0, 1)), 1.1))
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
                                   .field("loc")
                                   .within(Shape.center(new Point(new Position(0.5, 0.5)), 0.77))

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
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(new Position(0, 1)), 1))
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
                                   .field("loc")
                                   .near(0, 0)
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
    }

    @Test
    public void testNearMaxDistance() {
        getDs().ensureIndexes();
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        final Place found = getDs().find(Place.class)
                                   .field("loc")
                                   .near(0, 0, 1.5)
                                   .execute(new FindOptions().limit(1))
                                   .next();
        Assert.assertNotNull(found);
        final Place notFound = getDs().find(Place.class)
                                      .field("loc")
                                      .near(0, 0, 1)
                                      .execute(new FindOptions().limit(1))
                                      .tryNext();
        Assert.assertNull(notFound);
    }

    @Test(expected = MongoQueryException.class)
    public void testNearNoIndex() {
        final Place place1 = new Place("place1", new double[]{1, 1});
        getDs().save(place1);
        Place found = getDs().find(Place.class)
                             .field("loc")
                             .near(0, 0)
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
                                   .field("loc")
                                   .within(Shape.box(
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
                                   .field("loc")
                                   .within(Shape.box(
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
                                   .field("loc")
                                   .within(Shape.center(new Point(new Position(2, 2)), .4))
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
                                   .field("loc")
                                   .within(Shape.center(new Point(new Position(0, 1)), 1.1))
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
                                   .field("loc")
                                   .within(Shape.center(new Point(new Position(0.5, 0.5)), 0.77))
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
                                   .field("loc")
                                   .within(Shape.centerSphere(new Point(new Position(0, 1)), 1))
                                   .execute(new FindOptions().limit(1))
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
