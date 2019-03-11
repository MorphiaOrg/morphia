package dev.morphia.query;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This encapsulates the data necessary to define a shape for queries.
 */
public class Shape {
    private final String geometry;
    private final Point[] points;

    Shape(final String geometry, final Point... points) {
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
    public static Shape box(final Point bottomLeft, final Point upperRight) {
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
    public static Shape center(final Point center, final double radius) {
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
    public static Shape centerSphere(final Point center, final double radius) {
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
    public static Shape polygon(final Point... points) {
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

    /**
     * Creates a DBObject from this Shape
     *
     * @return the DBObject
     */
    public DBObject toDBObject() {
        final BasicDBList list = new BasicDBList();
        for (final Point point : points) {
            list.add(point.toDBObject());
        }

        return new BasicDBObject(geometry, list);
    }

    private Point[] copy(final Point[] array) {
        Point[] copy = new Point[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    /**
     * Defines a Point
     */
    public static class Point {
        private final double longitude;
        private final double latitude;

        /**
         * Creates a point using longitude and latitude values
         *
         * @param longitude the longitude
         * @param latitude  the latitude
         */
        public Point(final double longitude, final double latitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /**
         * Creates a DBObject from this Point
         *
         * @return the DBObject
         */
        public DBObject toDBObject() {
            final BasicDBList list = new BasicDBList();
            list.add(longitude);
            list.add(latitude);
            return list;
        }
    }

    private static class Center extends Shape {
        private final Point center;
        private final double radius;

        Center(final String geometry, final Point center, final double radius) {
            super(geometry);
            this.center = center;
            this.radius = radius;
        }

        @Override
        public DBObject toDBObject() {
            final BasicDBList list = new BasicDBList();
            list.add(center.toDBObject());
            list.add(radius);

            return new BasicDBObject(this.getGeometry(), list);
        }
    }
}
