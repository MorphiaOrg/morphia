package dev.morphia.query;


import com.mongodb.client.model.geojson.Point;
import dev.morphia.query.filters.Filters;

/**
 * This encapsulates the data necessary to define a shape for queries.
 *
 * @see Filters
 * @deprecated use the driver provide facilities instead.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class Shape {
    private final String geometry;
    private final Point[] points;

    Shape(String geometry, Point... points) {
        this.geometry = geometry;
        this.points = points;
    }

    /**
     * Specifies a rectangle for a geospatial $geoWithin query to return documents that are within the bounds of the rectangle,
     * according to their point-based location data.
     *
     * @param bottomLeft the bottom left bound
     * @param upperRight the upper right bound
     * @return the box
     * @mongodb.driver.manual reference/operator/query/box/ $box
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     */
    public static Shape box(Point bottomLeft, Point upperRight) {
        return new Shape("$box", bottomLeft, upperRight);
    }

    /**
     * Specifies a circle for a $geoWithin query.
     *
     * @param center the center of the circle
     * @param radius the radius circle
     * @return the box
     * @mongodb.driver.manual reference/operator/query/center/ $center
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     */
    public static Shape center(Point center, double radius) {
        return new Center("$center", center, radius);
    }

    /**
     * Specifies a circle for a geospatial query that uses spherical geometry.
     *
     * @param center the center of the circle
     * @param radius the radius circle
     * @return the box
     * @mongodb.driver.manual reference/operator/query/centerSphere/ $centerSphere
     */
    public static Shape centerSphere(Point center, double radius) {
        return new Center("$centerSphere", center, radius);
    }

    /**
     * Specifies a polygon for a geospatial $geoWithin query on legacy coordinate pairs.
     *
     * @param points the points of the polygon
     * @return the box
     * @mongodb.driver.manual reference/operator/query/polygon/ $polygon
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     */
    public static Shape polygon(Point... points) {
        return new Shape("$polygon", points);
    }

    /**
     * @return the geometry of the shape
     */
    public String getGeometry() {
        return geometry;
    }

    /**
     * @return the points of the shape
     */
    public Point[] getPoints() {
        return copy(points);
    }

    private Point[] copy(Point[] array) {
        Point[] copy = new Point[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    /**
     * @morphia.internal
     */
    public static class Center extends Shape {
        private final Point center;
        private final double radius;

        Center(String geometry, Point center, double radius) {
            super(geometry);
            this.center = center;
            this.radius = radius;
        }

        /**
         * @return the center
         */
        public Point getCenter() {
            return center;
        }

        /**
         * @return the radius
         */
        public double getRadius() {
            return radius;
        }
    }
}
