package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.GeoNear.geoNear;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestGeoNear extends AggregationTest {
    @Test
    public void testExample2() {
        getDatabase().getCollection(AGG_TEST_COLLECTION).createIndex(new Document("location", "2dsphere"));
        testPipeline(ServerVersion.ANY, true, false, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.99279, 40.719296)))
                        .distanceField("dist.calculated")
                        .maxDistance(2)
                        .query(eq("category", "Parks"))
                        .includeLocs("dist.location")
                        .spherical(true)));
    }

    @Test
    public void testExample3() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.99279, 40.719296)))
                        .distanceField("dist.calculated")
                        .maxDistance(2)
                        .query(eq("category", "Parks"))
                        .includeLocs("dist.location")
                        .spherical(true)));
    }

    @Test
    public void testExample4() {
        skipDataCheck();
        getDatabase().getCollection(AGG_TEST_COLLECTION).createIndex(new Document("location", "2dsphere"));
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.99279, 40.719296)))
                        .distanceField("location")
                        .maxDistance(2)
                        .query(eq("category", "Parks"))
                        .includeLocs("dist.location")
                        .spherical(true)));
    }

    @Test
    public void testExample5() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                lookup("places")
                        .let("pt", field("location"))
                        .pipeline(
                                geoNear(new Point(new Position(-73.98142, 40.71782)))
                                        .distanceField("distance"))
                        .as("joinedField"),
                match(eq("name", "Sara D. Roosevelt Park"))));
    }

    @Test
    public void testExample6() {
        skipDataCheck();
        getDatabase().getCollection(AGG_TEST_COLLECTION).createIndex(new Document("location", "2dsphere"));
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.98142, 40.71782)))
                        .key("location")
                        .distanceField("dist.calculated")
                        .query(eq("category", "Parks")),
                limit(5)));
    }

}
