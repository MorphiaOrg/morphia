package org.mongodb.morphia.geo;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a GeoJSON Point type.  Will be persisted into the database according to <a href="http://geojson.org/geojson-spec.html#id2">the
 * specification</a>. Therefore because of this, this entity will never have its own ID or store the its Class name.
 * <p/>
 * The builder for creating a Point is the {@code GeoJson.pointBuilder} method, or the helper {@code GeoJson.point} factory method.
 *
 * @see org.mongodb.morphia.geo.GeoJson#point(double, double)
 * @see GeoJson#pointBuilder()
 */
@Embedded
@Entity(noClassnameStored = true)
public class Point implements Geometry {
    private final double latitude;
    private final double longitude;

    @SuppressWarnings("unused") //needed for Morphia serialisation
    private Point() {
        longitude = 0;
        latitude = 0;
    }

    Point(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    Point(final List<Double> coordinates) {
        this(coordinates.get(1), coordinates.get(0));
    }

    @Override
    public List<Double> getCoordinates() {
        return Arrays.asList(longitude, latitude);
    }

    /**
     * Return the latitude of this point.
     *
     * @return the Point's latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Return the longitude of this point.
     *
     * @return the Point's longitude
     */
    public double getLongitude() {
        return longitude;
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

        if (Double.compare(point.latitude, latitude) != 0) {
            return false;
        }
        if (Double.compare(point.longitude, longitude) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Point{"
               + "latitude=" + latitude
               + ", longitude=" + longitude
               + '}';
    }
}
