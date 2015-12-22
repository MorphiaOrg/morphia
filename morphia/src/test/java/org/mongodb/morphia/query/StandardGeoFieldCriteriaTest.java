package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.testutil.JSONMatcher;

import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.geo.PointBuilder.pointBuilder;
import static org.mongodb.morphia.query.FilterOperator.NEAR;

public class StandardGeoFieldCriteriaTest extends TestBase {
    @Test
    public void shouldCreateCorrectNearQueryWithMaxDistance() {
        // given
        int maxDistanceMeters = 13;
        double latitude = 3.2;
        double longitude = 5.7;
        QueryImpl<Object> stubQuery = (QueryImpl<Object>) getDs().createQuery(Object.class);
        StandardGeoFieldCriteria criteria = new StandardGeoFieldCriteria(stubQuery, "location", NEAR, pointBuilder()
                                                                                                          .latitude(latitude)
                                                                                                          .longitude(longitude)
                                                                                                          .build(),
                                                                         maxDistanceMeters, false, false);

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
        QueryImpl<Object> stubQuery = (QueryImpl<Object>) getDs().createQuery(Object.class);

        StandardGeoFieldCriteria criteria = new StandardGeoFieldCriteria(stubQuery, "location", NEAR, pointBuilder()
                                                                                                          .latitude(latitude)
                                                                                                          .longitude(longitude)
                                                                                                          .build(),
                                                                         null, false, false);

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
