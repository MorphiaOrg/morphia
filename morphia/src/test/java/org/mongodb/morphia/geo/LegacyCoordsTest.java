package org.mongodb.morphia.geo;

import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.query.Query;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static org.mongodb.morphia.testutil.IndexMatcher.hasIndexNamed;
import static org.mongodb.morphia.testutil.JSONMatcher.jsonEqual;

/**
 * This test shows how to define an entity that uses the legacy co-ordinate pairs standard, which works with MongoDB server versions 2.2
 * and
 * earlier.  If you are using a server version higher than 2.2 (i.e. 2.4 and onwards) you should store location information as <a
 * href="http://docs.mongodb.org/manual/reference/glossary/#term-geojson">GeoJSON</a> and consult the documentation for indexes and queries
 * that work on this format.  Storing the location as GeoJSON gives you access to a wider range of queries.
 * <p/>
 * This set of tests should run on all server versions.
 */
public class LegacyCoordsTest extends TestBase {
    @Test
    public void shouldCreateA2dIndexOnAnEntityWithArrayOfCoordinates() {
        // given
        PlaceWithLegacyCoords pointA = new PlaceWithLegacyCoords(new double[]{3.1, 5.2}, "Point A");
        getDs().save(pointA);

        // when
        getDs().ensureIndexes();

        // then
        List<DBObject> indexes = getDs().getCollection(PlaceWithLegacyCoords.class).getIndexInfo();
        assertThat(indexes, hasIndexNamed("location_2d"));
    }

    @Test
    public void shouldFindPointWithExactMatch() {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        getDs().ensureIndexes();

        // when
        List<PlaceWithLegacyCoords> found = getDs().find(PlaceWithLegacyCoords.class)
                                                   .field("location")
                                                   .equal(new double[]{1.1, 2.3})
                                                   .asList();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(nearbyPlace));
    }

    @Test
    // Issue #275
    public void shouldGenerateCorrectQueryForNearSphereWithRadius() {
        // when
        Query<PlaceWithLegacyCoords> query = getDs().find(PlaceWithLegacyCoords.class)
                                                    .field("location")
                                                    .near(42.08563, -87.99822, 2, true);

        // then
        assertThat(query.getQueryObject().toString(),
                   jsonEqual("{ \"location\" : "
                             + "{ \"$nearSphere\" : [ 42.08563 , -87.99822] , "
                             + "\"$maxDistance\" : 2.0}}"));
    }

    @Test
    // Issue #275
    public void shouldGenerateCorrectQueryForNearWithMaxDistance() {
        // when
        Query<PlaceWithLegacyCoords> query = getDs().find(PlaceWithLegacyCoords.class)
                                                    .field("location")
                                                    .near(42.08563, -87.99822, 2);

        // then
        assertThat(query.getQueryObject().toString(),
                   jsonEqual("{ \"location\" : "
                             + "{ \"$near\" : [ 42.08563 , -87.99822] , "
                             + "\"$maxDistance\" : 2.0}}"));

    }

    @Test
    public void shouldNotReturnAnyResultsIfNoLocationsWithinGivenRadius() throws Exception {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        getDs().ensureIndexes();

        // when
        Query<PlaceWithLegacyCoords> locationQuery = getDs().find(PlaceWithLegacyCoords.class)
                                                            .field("location")
                                                            .near(1.0, 2.0, 0.1);
        // then
        assertThat(locationQuery.asList().size(), is(0));
        assertThat(locationQuery.get(), is(nullValue()));
    }

    @Test
    public void shouldReturnAllLocationsOrderedByDistanceFromQueryLocationWhenPerformingNearQuery() throws Exception {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        final PlaceWithLegacyCoords furtherAwayPlace = new PlaceWithLegacyCoords(new double[]{10.1, 12.3}, "Further Away Place");
        getDs().save(furtherAwayPlace);
        getDs().ensureIndexes();

        // when
        final List<PlaceWithLegacyCoords> found = getDs().find(PlaceWithLegacyCoords.class)
                                                         .field("location")
                                                         .near(1.0, 2.0)
                                                         .asList();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(2));
        assertThat(found.get(0), is(nearbyPlace));
        assertThat(found.get(1), is(furtherAwayPlace));
    }

    @Test
    public void shouldReturnOnlyThosePlacesWithinTheGivenRadius() throws Exception {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        final PlaceWithLegacyCoords furtherAwayPlace = new PlaceWithLegacyCoords(new double[]{10.1, 12.3}, "Further Away Place");
        getDs().save(furtherAwayPlace);
        getDs().ensureIndexes();

        // when
        final List<PlaceWithLegacyCoords> found = getDs().find(PlaceWithLegacyCoords.class)
                                                         .field("location")
                                                         .near(1.0, 2.0, 1.5)
                                                         .asList();
        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(nearbyPlace));
    }

    @Test(expected = MongoException.class)
    public void shouldThrowAnExceptionIfQueryingWithoutA2dIndex() throws Exception {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        List<DBObject> indexes = getDs().getCollection(PlaceWithLegacyCoords.class).getIndexInfo();
        assertThat(indexes, doesNotHaveIndexNamed("location_2d"));

        // when
        getDs().find(PlaceWithLegacyCoords.class)
               .field("location")
               .near(0, 0)
               .get();

        // then expect the Exception
    }
}
