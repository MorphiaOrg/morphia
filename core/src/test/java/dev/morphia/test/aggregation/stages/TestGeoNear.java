package dev.morphia.test.aggregation.stages;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.GeoNear.geoNear;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;

public class TestGeoNear extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, false, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.99279, 40.719296)))
                        .distanceField("dist.calculated")
                        .maxDistance(2)
                        .query(eq("category", "Parks"))
                        .includeLocs("dist.location")
                        .spherical(true)));
    }

    @Test
    public void testExample2() {
        // this example isn't representable in morphia as is
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.v60, true, true, (aggregation) -> aggregation.pipeline(
                lookup(EXAMPLE_TEST_COLLECTION)
                        .as("joinedField")
                        .let("pt", "$location")
                        .pipeline(
                                geoNear("$$pt")
                                        .distanceField("distance")),
                match(eq("name", "Sara D. Roosevelt Park"))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.98142, 40.71782)))
                        .distanceField("dist.calculated")
                        .key("location")
                        .query(eq("category", "Parks")),
                limit(5)));
    }

    @Test
    public void testExample5() {
        skipDataCheck();
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION).createIndex(new Document("location", "2dsphere"));
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                lookup(EXAMPLE_TEST_COLLECTION)
                        .as("joinedField")
                        .let("pt", "$location")
                        .pipeline(
                                geoNear(new Point(new Position(-73.98142, 40.71782)))
                                        .distanceField("distance")),
                match(eq("name", "Sara D. Roosevelt Park"))));
    }

    @Test
    public void testExample6() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                geoNear(new Point(new Position(-73.98142, 40.71782)))
                        .distanceField("dist.calculated")
                        .key("location")
                        .query(eq("category", "Parks")),
                limit(5)));
    }

}
