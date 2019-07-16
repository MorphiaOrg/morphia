package dev.morphia.query;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;
import org.bson.Document;
import org.junit.Test;

import static dev.morphia.query.FilterOperator.NEAR;
import static dev.morphia.query.Geo2dSphereCriteria.geo;
import static org.junit.Assert.assertThat;

public class Geo2dSphereCriteriaTest extends TestBase {
    @Test
    public void shouldCreateCorrectNearQueryWithMaxDistance() {
        // given
        double maxDistanceMeters = 13;
        double latitude = 3.2;
        double longitude = 5.7;
        QueryImpl<Object> stubQuery = (QueryImpl<Object>) getDs().find(Object.class);
        stubQuery.disableValidation();
        Geo2dSphereCriteria criteria = geo(getMapper(), stubQuery, "location", NEAR, new Point(new Position(latitude, longitude)))
                                           .maxDistance(maxDistanceMeters);

        // when
        Document queryDocument = criteria.toDocument();

        // then
        assertThat(queryDocument.toString(), JSONMatcher.jsonEqual("  { location : "
                                                                   + "  { $near : "
                                                                   + "    { $geometry : "
                                                                   + "      { type : 'Point' , "
                                                                   + "        coordinates : [ " + longitude + " , " + latitude + "]"
                                                                   + "      }, "
                                                                   + "      $maxDistance : " + maxDistanceMeters
                                                                   + "    }"
                                                                   + "  }"
                                                                   + "}"));
    }

    @Test
    public void shouldCreateCorrectNearQueryWithoutMaxDistance() {
        // given
        double latitude = 3.2;
        double longitude = 5.7;
        QueryImpl<Object> stubQuery = (QueryImpl<Object>) getDs().find(Object.class);
        stubQuery.disableValidation();

        Geo2dSphereCriteria criteria = geo(getMapper(), stubQuery, "location", NEAR, new Point(new Position(latitude, longitude)));

        // when
        Document queryDocument = criteria.toDocument();


        // then
        assertThat(queryDocument.toString(), JSONMatcher.jsonEqual("  { location : "
                                                                   + "  { $near : "
                                                                   + "    { $geometry : "
                                                                   + "      { type : 'Point' , "
                                                                   + "        coordinates : [ " + longitude + " , " + latitude + "]"
                                                                   + "      } "
                                                                   + "    }"
                                                                   + "  }"
                                                                   + "}"));
    }
}
