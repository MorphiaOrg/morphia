package dev.morphia.geo;

import com.mongodb.MongoException;
import dev.morphia.query.QueryImpl;
import org.bson.Document;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static dev.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static dev.morphia.testutil.IndexMatcher.hasIndexNamed;
import static dev.morphia.testutil.JSONMatcher.jsonEqual;

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
        List<Document> indexes = getIndexInfo(PlaceWithLegacyCoords.class);
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
                                                   .execute().toList();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(nearbyPlace));
    }

    @Test
    public void shouldGenerateCorrectQueryForNearSphereWithRadius() {
        // when
        Query<PlaceWithLegacyCoords> query = getDs().find(PlaceWithLegacyCoords.class)
                                                    .field("location")
                                                    .near(-87.99822, 42.08563, 2, true);

        // then
        assertThat(((QueryImpl) query).getQueryDocument().toString(),
                   jsonEqual("{ \"location\" : "
                             + "{ \"$nearSphere\" : [ 42.08563 , -87.99822] , "
                             + "\"$maxDistance\" : 2.0}}"));
    }

    @Test
    public void shouldGenerateCorrectQueryForNearWithMaxDistance() {
        // when
        Query<PlaceWithLegacyCoords> query = getDs().find(PlaceWithLegacyCoords.class)
                                                    .field("location")
                                                    .near(-87.99822, 42.08563, 2);

        // then
        assertThat(((QueryImpl) query).getQueryDocument().toString(),
                   jsonEqual("{ \"location\" : "
                             + "{ \"$near\" : [ 42.08563 , -87.99822] , "
                             + "\"$maxDistance\" : 2.0}}"));

    }

    @Test
    public void shouldNotReturnAnyResultsIfNoLocationsWithinGivenRadius() {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        getDs().ensureIndexes();

        // when
        Query<PlaceWithLegacyCoords> locationQuery = getDs().find(PlaceWithLegacyCoords.class)
                                                            .field("location")
                                                            .near(1.0, 2.0, 0.1);
        // then
        assertThat(locationQuery.count(), is(0L));
        assertThat(locationQuery.execute(new FindOptions().limit(1)).tryNext(), is(nullValue()));
    }

    @Test
    public void shouldReturnAllLocationsOrderedByDistanceFromQueryLocationWhenPerformingNearQuery() {
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
                                                         .execute().toList();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(2));
        assertThat(found.get(0), is(nearbyPlace));
        assertThat(found.get(1), is(furtherAwayPlace));
    }

    @Test
    public void shouldReturnOnlyThosePlacesWithinTheGivenRadius() {
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
                                                         .execute().toList();
        // then
        assertThat(found, is(notNullValue()));
        assertThat(found.size(), is(1));
        assertThat(found.get(0), is(nearbyPlace));
    }

    @Test(expected = MongoException.class)
    public void shouldThrowAnExceptionIfQueryingWithoutA2dIndex() {
        // given
        final PlaceWithLegacyCoords nearbyPlace = new PlaceWithLegacyCoords(new double[]{1.1, 2.3}, "Nearby Place");
        getDs().save(nearbyPlace);
        List<Document> indexes = getIndexInfo(PlaceWithLegacyCoords.class);
        assertThat(indexes, doesNotHaveIndexNamed("location_2d"));

        // when
        getDs().find(PlaceWithLegacyCoords.class)
               .field("location")
               .near(0, 0)
               .execute(new FindOptions().limit(1))
               .tryNext();

        // then expect the Exception
    }
}
