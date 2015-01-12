package org.mongodb.morphia.geo;

/**
 * Factory class for creating GeoJSON types.  See 
 * <a href="http://docs.mongodb.org/manual/applications/geospatial-indexes/#geojson-objects">the
 * documentation</a> for all the types.
 */
public final class GeoJson {
    private GeoJson() {
    }

    /**
     * Create a new Point representing a <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a> point
     * type.  For a safer way to create points with latitude and longitude coordinates without mixing up the order, see pointBuilder().
     * <p/>
     * Supported by server versions 2.4 and above.
     *
     * @param latitude  the point's latitude coordinate
     * @param longitude the point's longitude coordinate
     * @return a Point instance representing a single location point defined by the given latitude and longitude
     * @see org.mongodb.morphia.geo.PointBuilder
     */
    public static Point point(final double latitude, final double longitude) {
        return new Point(latitude, longitude);
    }

    /**
     * Create a new LineString representing a <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * LineString type.  Supported by server versions 2.4 an above.
     *
     * @param points an ordered series of Points that make up the line
     * @return a LineString instance representing a series of ordered points that make up a line
     */
    public static LineString lineString(final Point... points) {
        return new LineString(points);
    }

    /**
     * Create a new Polygon representing a <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * Polygon type. This helper method uses {@code org.mongodb.morphia.geo.GeoJson#polygon(LineString, LineString...)} to create the
     * Polygon.  If you need to create Polygons with interior rings (holes), use that method.
     * <p/>
     * Supported by server versions 2.4 and above.
     *
     * @param points an ordered series of Points that make up the polygon.  The first and last points should be the same to close the
     *               polygon
     * @return a Polygon as defined by the points.
     * @throws java.lang.IllegalArgumentException if the start and end points are not the same
     * @see org.mongodb.morphia.geo.GeoJson#polygon(LineString, LineString...)
     */
    public static Polygon polygon(final Point... points) {
        LineString exteriorBoundary = lineString(points);
        ensurePolygonIsClosed(exteriorBoundary);
        return new Polygon(exteriorBoundary);
    }

    /**
     * Lets you create a Polygon representing a 
     * <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * Polygon type. This method is especially useful for defining polygons with inner rings.
     * <p/>
     * Supported by server versions 2.4 and above.
     *
     * @param exteriorBoundary   a LineString that contains a series of Points that make up the polygon.  The first and last points should
     *                           be the same to close the polygon
     * @param interiorBoundaries optional varargs that let you define the boundaries for any holes inside the polygon
     * @return a PolygonBuilder to be used to build up the required Polygon
     * @throws java.lang.IllegalArgumentException if the start and end points are not the same
     */
    public static Polygon polygon(final LineString exteriorBoundary, final LineString... interiorBoundaries) {
        ensurePolygonIsClosed(exteriorBoundary);
        for (final LineString boundary : interiorBoundaries) {
            ensurePolygonIsClosed(boundary);
        }
        return new Polygon(exteriorBoundary, interiorBoundaries);
    }

    /**
     * Create a new MultiPoint representing a <a href="http://docs.mongodb.org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * MultiPoint type.  Supported by server versions 2.6 and above.
     *
     * @param points a set of points that make up the MultiPoint object
     * @return a MultiPoint object containing all the given points
     */
    public static MultiPoint multiPoint(final Point... points) {
        return new MultiPoint(points);
    }

    /**
     * Create a new MultiLineString representing a <a href="http://docs.mongodb
     * .org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * MultiLineString type.  Supported by server versions 2.6 and above.
     *
     * @param lines a set of lines that make up the MultiLineString object
     * @return a MultiLineString object containing all the given lines
     */
    public static MultiLineString multiLineString(final LineString... lines) {
        return new MultiLineString(lines);
    }

    /**
     * Create a new MultiPolygon representing a <a href="http://docs.mongodb
     * .org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * MultiPolygon type.  Supported by server versions 2.6 and above.
     *
     * @param polygons a series of polygons (which may contain inner rings)
     * @return a MultiPolygon object containing all the given polygons
     */
    public static MultiPolygon multiPolygon(final Polygon... polygons) {
        return new MultiPolygon(polygons);
    }

    /**
     * Return a GeometryCollection that will let you create a GeometryCollection <a href="http://docs.mongodb
     * .org/manual/apps/geospatial-indexes/#geojson-objects">GeoJSON</a>
     * GeometryCollection.  Supported by server version 2.6
     *
     * @param geometries a series of Geometry instances that will make up this GeometryCollection
     * @return a GeometryCollection made up of all the geometries
     */
    public static GeometryCollection geometryCollection(final Geometry... geometries) {
        return new GeometryCollection(geometries);
    }

    private static void ensurePolygonIsClosed(final LineString points) {
        int size = points.getCoordinates().size();
        if (size > 0 && !points.getCoordinates().get(0).equals(points.getCoordinates().get(size - 1))) {
            throw new IllegalArgumentException("A polygon requires the starting point to be the same as the end to ensure a closed "
                                               + "area");
        }
    }
}
