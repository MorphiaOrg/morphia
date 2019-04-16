package dev.morphia.geo;

import com.mongodb.client.model.geojson.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating GeoJSON types.  See <a href="http://docs.mongodb
 * .org/manual/applications/geospatial-indexes/#geojson-objects">the
 * documentation</a> for all the types.
 */
public final class GeoJson {
    private GeoJson() {
    }

    /**
     * Create a new Point representing a GeoJSON point type.  For a safer way to create points with latitude and longitude coordinates
     * without mixing up the order, {@link PointBuilder}.
     *
     * @param latitude  the point's latitude coordinate
     * @param longitude the point's longitude coordinate
     * @return a Point instance representing a single location point defined by the given latitude and longitude
     * @mongodb.server.release 2.4
     * @see dev.morphia.geo.PointBuilder
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static Point point(final double latitude, final double longitude) {
        return new Point(latitude, longitude);
    }

    /**
     * Create a new Polygon representing a GeoJSON Polygon type. This helper method uses {@link #polygon(LineString, LineString...)} to
     * create the Polygon.  If you need to create Polygons with interior rings (holes), use that method.
     *
     * @param points an ordered series of Points that make up the polygon.  The first and last points should be the same to close the
     *               polygon
     * @return a Polygon as defined by the points.
     * @throws java.lang.IllegalArgumentException if the start and end points are not the same
     * @mongodb.server.release 2.4
     * @see dev.morphia.geo.GeoJson#polygon(LineString, LineString...)
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static Polygon polygon(final Point... points) {
        LineString exteriorBoundary = lineString(points);
        ensurePolygonIsClosed(exteriorBoundary);
        return new Polygon(exteriorBoundary);
    }

    /**
     * Create a new LineString representing a GeoJSON LineString type.
     *
     * @param points an ordered series of Points that make up the line
     * @return a LineString instance representing a series of ordered points that make up a line
     * @mongodb.server.release 2.4
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static LineString lineString(final Point... points) {
        return new LineString(points);
    }

    private static void ensurePolygonIsClosed(final LineString points) {
        int size = points.getCoordinates().size();
        if (size > 0 && !points.getCoordinates().get(0).equals(points.getCoordinates().get(size - 1))) {
            throw new IllegalArgumentException("A polygon requires the starting point to be the same as the end to ensure a closed "
                                               + "area");
        }
    }

    /**
     * Lets you create a Polygon representing a GeoJSON Polygon type. This method is especially useful for defining polygons with inner
     * rings.
     *
     * @param exteriorBoundary   a LineString that contains a series of Points that make up the polygon.  The first and last points should
     *                           be the same to close the polygon
     * @param interiorBoundaries optional varargs that let you define the boundaries for any holes inside the polygon
     * @return a PolygonBuilder to be used to build up the required Polygon
     * @throws java.lang.IllegalArgumentException if the start and end points are not the same
     * @mongodb.server.release 2.4
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static Polygon polygon(final LineString exteriorBoundary, final LineString... interiorBoundaries) {
        ensurePolygonIsClosed(exteriorBoundary);
        for (final LineString boundary : interiorBoundaries) {
            ensurePolygonIsClosed(boundary);
        }
        return new Polygon(exteriorBoundary, interiorBoundaries);
    }

    /**
     * Create a new MultiPoint representing a GeoJSON MultiPoint type.
     *
     * @param points a set of points that make up the MultiPoint object
     * @return a MultiPoint object containing all the given points
     * @mongodb.server.release 2.6
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static MultiPoint multiPoint(final Point... points) {
        return new MultiPoint(points);
    }

    /**
     * Create a new MultiLineString representing a GeoJSON MultiLineString type.
     *
     * @param lines a set of lines that make up the MultiLineString object
     * @return a MultiLineString object containing all the given lines
     * @mongodb.server.release 2.6
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static MultiLineString multiLineString(final LineString... lines) {
        return new MultiLineString(lines);
    }

    /**
     * Create a new MultiPolygon representing a GeoJSON MultiPolygon type.
     *
     * @param polygons a series of polygons (which may contain inner rings)
     * @return a MultiPolygon object containing all the given polygons
     * @mongodb.server.release 2.6
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static MultiPolygon multiPolygon(final Polygon... polygons) {
        return new MultiPolygon(polygons);
    }

    /**
     * Return a GeometryCollection that will let you create a GeoJSON GeometryCollection.
     *
     * @param geometries a series of Geometry instances that will make up this GeometryCollection
     * @return a GeometryCollection made up of all the geometries
     * @mongodb.server.release 2.6
     * @see <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     */
    public static GeometryCollection geometryCollection(final Geometry... geometries) {
        return new GeometryCollection(geometries);
    }

    /**
     * @param values the values to convert
     * @morphia.internal
     * @return the converted values
     */
    public static List<Position> convertPoints(final List<Point> values) {
        final ArrayList<Position> positions = new ArrayList<Position>();
        for (final Point point : values) {
            positions.add(new Position(point.getLongitude(), point.getLatitude()));
        }

        return positions;
    }

    /**
     * @param values the values to convert
     * @morphia.internal
     * @return the converted values
     */
    public static List<List<Position>> convertLineStrings(final List<LineString> values) {
        final List<List<Position>> positions = new ArrayList<List<Position>>();
        for (final LineString line : values) {
            positions.add(convertPoints(line.getCoordinates()));
        }

        return positions;
    }
}
