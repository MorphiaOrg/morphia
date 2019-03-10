package dev.morphia.geo;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a collection of mixed GeoJson objects as per the <a href="http://geojson.org/geojson-spec
 * .html#geometrycollection">GeoJSON
 * specification</a>. Therefore this entity will never have its own ID or store the its Class name.
 * <p/>
 * The factory for creating a MultiPoint is the {@code GeoJson.multiPoint} method.
 *
 * @see dev.morphia.geo.GeoJson
 */
@Embedded
@Entity(noClassnameStored = true)
public class GeometryCollection {
    private final String type = "GeometryCollection";
    private final List<Geometry> geometries;

    @SuppressWarnings("UnusedDeclaration") // needed by morphia
    private GeometryCollection() {
        geometries = new ArrayList<Geometry>();
    }

    GeometryCollection(final List<Geometry> geometries) {
        this.geometries = geometries;
    }

    GeometryCollection(final Geometry... geometries) {
        this.geometries = Arrays.asList(geometries);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + geometries.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GeometryCollection that = (GeometryCollection) o;

        if (!geometries.equals(that.geometries)) {
            return false;
        }
        if (!type.equals(that.type)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "GeometryCollection{"
               + "type='" + type + '\''
               + ", geometries=" + geometries
               + '}';
    }
}
