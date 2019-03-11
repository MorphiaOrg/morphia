package dev.morphia.query;

import org.junit.Test;
import dev.morphia.geo.GeoJson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static dev.morphia.geo.GeoJson.point;

/**
 * Unit test - more complete testing that uses the GeoJson factory is contained in functional Geo tests.
 */
public class GeoJsonTest {
    @Test(expected = IllegalArgumentException.class)
    public void shouldErrorIfStartAndEndOfPolygonAreNotTheSame() {
        // expect
        GeoJson.polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0));
    }

    @Test
    public void shouldNotErrorIfPolygonIsEmpty() {
        // expect
        assertThat(GeoJson.polygon(), is(notNullValue()));
    }

}
