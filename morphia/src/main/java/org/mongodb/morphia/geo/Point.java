package org.mongodb.morphia.geo;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GeoJSON Point type.  Will be persisted into the database according to <a href="http://geojson.org/geojson-spec.html#id2">the
 * specification</a>. Therefore because of this, this entity will never have its own ID or store the its Class name.
 * <p/>
 * The builder for creating a Point is the {@code GeoJson.pointBuilder} method, or the helper {@code GeoJson.point} factory method.
 *
 * @see org.mongodb.morphia.geo.GeoJson#point(double, double)
 */
@Embedded
@Entity(noClassnameStored = true)
public class Point implements Geometry {
    private final List<Double> coordinates = new ArrayList<Double>();

    Point(final double latitude, final double longitude) {
        coordinates.add(longitude);
        coordinates.add(latitude);
    }

    Point(final List<Double> coordinates) {
        this.coordinates.addAll(coordinates);
    }

    @Override
    public List<Double> getCoordinates() {
        return coordinates;
    }

    /**
     * Return the latitude of this point.
     *
     * @return the Point's latitude
     */
    public double getLatitude() {
        return coordinates.get(1);
    }

    /**
     * Return the longitude of this point.
     *
     * @return the Point's longitude
     */
    public double getLongitude() {
        return coordinates.get(0);
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

        Point point = (Point) o;

        if (getCoordinates().size() != point.getCoordinates().size()) {
            return false;
        }
        for (int i = 0; i < coordinates.size(); i++) {
            final Double coordinate = coordinates.get(i);
            if (Double.compare(coordinate, point.getCoordinates().get(i)) != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("Point{coordinates=%s}", coordinates);
    }
}
