package dev.morphia.test.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.centerSphere;

public class TestCenterSphere extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/centerSphere/findPlacesWithinTenMiles
     */
    @Test
    @DisplayName("Find Places Within Ten Miles")
    public void testFindPlacesWithinTenMiles() {
        testQuery((query) -> query.filter(centerSphere("loc", new Point(new Position(-88, 30)), 10 / 3963.2)));
    }
}
