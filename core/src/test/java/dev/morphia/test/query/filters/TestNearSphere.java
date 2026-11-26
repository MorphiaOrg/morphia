package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.nearSphere;

public class TestNearSphere extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/nearSphere/specifyCenterPointUsingGeoJson
     */
    @Test
    @DisplayName("Specify Center Point Using GeoJSON")
    public void testSpecifyCenterPointUsingGeoJson() {
        testQuery((query) -> query.filter(nearSphere("location", new Point(new Position(-73.9667, 40.78)))
                .minDistance(1000.0).maxDistance(5000.0)));
    }

}