package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static dev.morphia.geo.PointBuilder.pointBuilder;
import static dev.morphia.query.FilterOperator.NEAR;

public class Geo2dSphereCriteriaTest extends TestBase {
    @Test
    public void shouldCreateCorrectNearQueryWithMaxDistance() {
        // given
        double maxDistanceMeters = 13;
        double latitude = 3.2;
        double longitude = 5.7;
        QueryImpl<Object> stubQuery = (QueryImpl<Object>) getDs().find(Object.class);
        stubQuery.disableValidation();
        Geo2dSphereCriteria criteria = Geo2dSphereCriteria.geo(stubQuery, "location", NEAR, pointBuilder()
                                                                                                          .latitude(latitude)
                                                                                                          .longitude(longitude)
                                                                                                          .build())
                                                          .maxDistance(maxDistanceMeters);

        // when
        DBObject queryDocument = criteria.toDBObject();

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

        Geo2dSphereCriteria criteria = Geo2dSphereCriteria.geo(stubQuery, "location", NEAR, pointBuilder()
                                                                                                          .latitude(latitude)
                                                                                                          .longitude(longitude)
                                                                                                          .build());

        // when
        DBObject queryDocument = criteria.toDBObject();


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
