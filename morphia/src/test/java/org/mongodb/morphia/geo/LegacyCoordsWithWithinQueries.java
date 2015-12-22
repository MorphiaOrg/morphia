package org.mongodb.morphia.geo;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.query.Shape;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Although this tests the old legacy coordinate system of storing location, this set of tests shows the functionality that's available
 * with
 * these coordinates in later versions of the server that also support GeoJSON.  In order to get full geo querying functionality, you
 * should
 * use GeoJSON for storing your location not legacy co-ordinates.
 * <p/>
 * This test requires server version 2.4 or above as it uses $geoWithin.
 */
public class LegacyCoordsWithWithinQueries extends TestBase {
    @Test
    public void shouldNotReturnAnyPointsIfNothingInsideCircle() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with circle that does not cover the only point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.center(new Shape.Point(2, 2), 0.5))
                                                   .get();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldNotReturnAnyValuesWhenTheQueryBoxDoesNotContainAnyPoints() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with a box that does not cover the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.box(new Shape.Point(0, 0), new Shape.Point(0.5, 0.5)))
                                                   .get();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldNotReturnAnyValuesWhenTheQueryPolygonDoesNotContainAnyPoints() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{7.3, 9.2}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with polygon that's nowhere near the given point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.polygon(new Shape.Point(0, 0),
                                                                         new Shape.Point(0, 5),
                                                                         new Shape.Point(2, 3),
                                                                         new Shape.Point(1, 0)))
                                                   .get();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldReturnAPointThatIsFullyWithinQueryPolygon() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with polygon that contains expected point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.polygon(new Shape.Point(0, 0),
                                                                         new Shape.Point(0, 5),
                                                                         new Shape.Point(2, 3),
                                                                         new Shape.Point(1, 0)))
                                                   .get();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnOnlyThePointsWithinTheGivenCircle() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Near point");
        getDs().save(expectedPoint);
        final PlaceWithLegacyCoords otherPoint = new PlaceWithLegacyCoords(new double[]{3.1, 5.2}, "Further point");
        getDs().save(otherPoint);
        getDs().ensureIndexes();

        // when
        final List<PlaceWithLegacyCoords> found = getDs().find(PlaceWithLegacyCoords.class)
                                                         .field("location")
                                                         .within(Shape.center(new Shape.Point(1, 2), 1.1))
                                                         .asList();

        // then
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(expectedPoint));
    }

    @Test
    public void shouldReturnPointOnBoundaryOfQueryCircle() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with circle with an edge that exactly covers the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.center(new Shape.Point(0, 1), 1))
                                                   .get();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnPointOnBoundaryOfQueryCircleWithSphericalGeometry() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with circle with an edge that exactly covers the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.centerSphere(new Shape.Point(0, 1), 1))
                                                   .get();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnPointThatIsFullyInsideTheQueryBox() throws Exception {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with a box that covers the whole point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.box(new Shape.Point(0, 0), new Shape.Point(2, 2)))
                                                   .get();
        // then
        assertThat(found, is(expectedPoint));
    }
}
