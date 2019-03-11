package dev.morphia.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a series of points, which will saved into MongoDB as per the <a href="http://geojson.org/geojson-spec
 * .html#id5">GeoJSON specification</a>.
 * <p/>
 * The factory for creating a MultiPoint is the {@code GeoJson.multiPoint} method.
 *
 * @see dev.morphia.geo.GeoJson#multiPoint(Point...)
 */
public class MultiPoint implements Geometry {
    private final List<Point> coordinates;

    @SuppressWarnings("UnusedDeclaration") // used by Morphia
    private MultiPoint() {
        this.coordinates = new ArrayList<Point>();
    }

    MultiPoint(final Point... points) {
        this.coordinates = Arrays.asList(points);
    }

    MultiPoint(final List<Point> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public List<Point> getCoordinates() {
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

        MultiPoint that = (MultiPoint) o;

        if (!coordinates.equals(that.coordinates)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "MultiPoint{"
               + "coordinates=" + coordinates
               + '}';
    }

    @Override
    public com.mongodb.client.model.geojson.MultiPoint convert() {
        return convert(null);
    }

    @Override
    public com.mongodb.client.model.geojson.MultiPoint convert(final CoordinateReferenceSystem crs) {
        return new com.mongodb.client.model.geojson.MultiPoint(crs != null ? crs.convert() : null,
            GeoJson.convertPoints(coordinates));
    }
}
