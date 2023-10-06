package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.Point;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class PolygonFilter extends Filter {
    private final Point[] points;

    /**
     * @param field  the field
     * @param points the points
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    PolygonFilter(String field, Point[] points) {
        super("$polygon", field, null);
        this.points = points;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the points
     */
    @MorphiaInternal
    public Point[] points() {
        return points;
    }
}
