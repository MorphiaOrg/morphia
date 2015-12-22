package org.mongodb.morphia.geo;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.utils.IndexDirection;

public class GeoJsonIndexTest extends TestBase {
    @Test(expected = Exception.class)
    public void shouldErrorWhenCreatingA2dIndexOnGeoJson() {
        // given
        Place pointB = new Place(GeoJson.point(3.1, 7.5), "Point B");
        getDs().save(pointB);

        // when
        getDs().ensureIndexes();
        //"location object expected, location array not in correct format", code : 13654
    }

    @SuppressWarnings("unused")
    private static final class Place {
        @Indexed(IndexDirection.GEO2D)
        private Point location;
        private String name;

        private Place(final Point location, final String name) {
            this.location = location;
            this.name = name;
        }

        private Place() {
        }
    }
}
