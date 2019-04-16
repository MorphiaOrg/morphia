package dev.morphia.geo;

import com.mongodb.client.model.geojson.PolygonCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a set of polygons, which will saved into MongoDB as per the <a href="http://geojson.org/geojson-spec
 * .html#id7">GeoJSON
 * specification</a>.
 * <p/>
 * The factory for creating a MultiPolygon is the {@code GeoJson.multiPolygon} method.
 *
 * @see dev.morphia.geo.GeoJson#multiPolygon(Polygon...)
 */
public class MultiPolygon implements Geometry {
    private final List<Polygon> coordinates;

    @SuppressWarnings("UnusedDeclaration") // used by Morphia
    private MultiPolygon() {
        coordinates = new ArrayList<Polygon>();
    }

    MultiPolygon(final Polygon... polygons) {
        coordinates = Arrays.asList(polygons);
    }

    MultiPolygon(final List<Polygon> polygons) {
        coordinates = polygons;
    }

    @Override
    public List<Polygon> getCoordinates() {
        return coordinates;
    }

    @Override
    public int hashCode() {
        return coordinates.hashCode();
    }

    /* equals, hashCode and toString. Useful primarily for testing and debugging. Don't forget to re-create when changing this class */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MultiPolygon that = (MultiPolygon) o;

        if (!coordinates.equals(that.coordinates)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "MultiPolygon{"
               + "coordinates=" + coordinates
               + '}';
    }

    @Override
    public com.mongodb.client.model.geojson.MultiPolygon convert() {
        return convert(null);
    }

    @Override
    public com.mongodb.client.model.geojson.MultiPolygon convert(final CoordinateReferenceSystem crs) {
        List<PolygonCoordinates> coords = new ArrayList<PolygonCoordinates>();
        for (final Polygon list : coordinates) {
            coords.add(list.convert(crs).getCoordinates());
        }
        return new com.mongodb.client.model.geojson.MultiPolygon(crs != null ? crs.convert() : null, coords);
    }
}
