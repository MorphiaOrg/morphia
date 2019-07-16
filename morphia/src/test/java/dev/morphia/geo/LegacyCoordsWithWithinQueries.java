package dev.morphia.geo;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Shape;
import org.junit.Test;

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
    public void shouldNotReturnAnyPointsIfNothingInsideCircle() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with circle that does not cover the only point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.center(new Point(new Position(2, 2)), 0.5))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldNotReturnAnyValuesWhenTheQueryBoxDoesNotContainAnyPoints() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with a box that does not cover the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.box(new Point(new Position(0, 0)), new Point(new Position(0.5, 0.5))))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldNotReturnAnyValuesWhenTheQueryPolygonDoesNotContainAnyPoints() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords point = new PlaceWithLegacyCoords(new double[]{7.3, 9.2}, "place");
        getDs().save(point);
        getDs().ensureIndexes();

        // when - search with polygon that's nowhere near the given point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.polygon(new Point(new Position(0, 0)),
                                                                         new Point(new Position(0, 5)),
                                                                         new Point(new Position(2, 3)),
                                                                         new Point(new Position(1, 0))))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(nullValue()));
    }

    @Test
    public void shouldReturnAPointThatIsFullyWithinQueryPolygon() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with polygon that contains expected point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.polygon(new Point(new Position(0, 0)),
                                                                         new Point(new Position(0, 5)),
                                                                         new Point(new Position(2, 3)),
                                                                         new Point(new Position(1, 0))))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnOnlyThePointsWithinTheGivenCircle() {
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
                                                         .within(Shape.center(new Point(new Position(1, 2)), 1.1))
                                                         .execute().toList();

        // then
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(expectedPoint));
    }

    @Test
    public void shouldReturnPointOnBoundaryOfQueryCircle() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with circle with an edge that exactly covers the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.center(new Point(new Position(0, 1)), 1))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnPointOnBoundaryOfQueryCircleWithSphericalGeometry() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with circle with an edge that exactly covers the point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.centerSphere(new Point(new Position(0, 1)), 1))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(expectedPoint));
    }

    @Test
    public void shouldReturnPointThatIsFullyInsideTheQueryBox() {
        // given
        checkMinServerVersion(2.4);

        final PlaceWithLegacyCoords expectedPoint = new PlaceWithLegacyCoords(new double[]{1, 1}, "place");
        getDs().save(expectedPoint);
        getDs().ensureIndexes();

        // when - search with a box that covers the whole point
        final PlaceWithLegacyCoords found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .within(Shape.box(
                                                       new Point(new Position(0, 0)),
                                                       new Point(new Position(2, 2))))
                                                   .execute(new FindOptions().limit(1))
                                                   .tryNext();
        // then
        assertThat(found, is(expectedPoint));
    }
}
