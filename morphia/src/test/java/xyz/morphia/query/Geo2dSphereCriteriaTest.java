package xyz.morphia.query;

import com.mongodb.BasicDBObject;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static xyz.morphia.geo.PointBuilder.pointBuilder;
import static xyz.morphia.query.FilterOperator.NEAR;

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
        BasicDBObject queryDocument = new BasicDBObject();
        criteria.addTo(queryDocument);

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
        BasicDBObject queryDocument = new BasicDBObject();
        criteria.addTo(queryDocument);


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
