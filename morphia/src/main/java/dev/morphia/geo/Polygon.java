package dev.morphia.geo;

import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents either a simple polygon enclosing an area, or a more complex polygon that contains both an exterior boundary and
 * interior boundaries (holes) within it.  It will be persisted into the database according to <a
 * href="http://geojson.org/geojson-spec.html#id4">the specification</a>.
 * <p/>
 * The factory for creating a Polygon is {@code PolygonBuilder}, which is accessible via the {@code GeoJson.polygonBuilder} method.
 * Alternatively, simple polygons without inner rings can be created via the {@code GeoJson.polygon} factory method.
 *
 * @see dev.morphia.geo.GeoJson#polygon(LineString, LineString...)
 * @see dev.morphia.geo.GeoJson#polygon(Point...)
 */
public class Polygon implements Geometry {
    private final LineString exteriorBoundary;
    private final List<LineString> interiorBoundaries;

    @SuppressWarnings("UnusedDeclaration") // used by Morphia
    private Polygon() {
        exteriorBoundary = null;
        interiorBoundaries = new ArrayList<LineString>();
    }

    Polygon(final LineString exteriorBoundary, final LineString... interiorBoundaries) {
        this.exteriorBoundary = exteriorBoundary;
        this.interiorBoundaries = Arrays.asList(interiorBoundaries);
    }

    Polygon(final List<LineString> boundaries) {
        exteriorBoundary = boundaries.get(0);
        if (boundaries.size() > 1) {
            interiorBoundaries = boundaries.subList(1, boundaries.size());
        } else {
            interiorBoundaries = new ArrayList<LineString>();
        }
    }

    @Override
    public List<LineString> getCoordinates() {
        List<LineString> polygonBoundaries = new ArrayList<LineString>();
        polygonBoundaries.add(exteriorBoundary);
        polygonBoundaries.addAll(interiorBoundaries);
        return polygonBoundaries;
    }

    /**
     * Returns a LineString representing the exterior boundary of this Polygon.  Polygons should have an exterior boundary where the end
     * point is the same as the start point.
     *
     * @return a LineString containing the points that make up the external boundary of this Polygon.
     */
    public LineString getExteriorBoundary() {
        return exteriorBoundary;
    }

    /**
     * Returns a (possibly empty) List of LineStrings, one for each hole inside the external boundary of this polygon.
     *
     * @return a List of LineStrings where each LineString represents an internal boundary or hole.
     */
    public List<LineString> getInteriorBoundaries() {
        return Collections.unmodifiableList(interiorBoundaries);
    }

    @Override
    public int hashCode() {
        int result = exteriorBoundary.hashCode();
        result = 31 * result + interiorBoundaries.hashCode();
        return result;
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

        Polygon polygon = (Polygon) o;

        if (!exteriorBoundary.equals(polygon.exteriorBoundary)) {
            return false;
        }
        if (!interiorBoundaries.equals(polygon.interiorBoundaries)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Polygon{"
               + "exteriorBoundary=" + exteriorBoundary
               + ", interiorBoundaries=" + interiorBoundaries
               + '}';
    }

    @Override
    public com.mongodb.client.model.geojson.Polygon convert() {
        return convert(null);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public com.mongodb.client.model.geojson.Polygon convert(final CoordinateReferenceSystem crs) {
        final List<List<Position>> lists = GeoJson.convertLineStrings(interiorBoundaries);
        final List[] holeArray = lists.toArray(new List[0]);
        return new com.mongodb.client.model.geojson.Polygon(crs != null ? crs.convert() : null,
            new PolygonCoordinates(exteriorBoundary.convert().getCoordinates(), holeArray));
    }
}
