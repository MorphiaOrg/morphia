package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class CenterFilter extends Filter {
    private final double radius;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected CenterFilter(String filterName, String field, Point value, double radius) {
        super(filterName, field, value);
        this.radius = radius;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    @NonNull
    public Point getValue() {
        Object value = super.getValue();
        if (value != null) {
            return (Point) value;
        }
        throw new NullPointerException();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public double radius() {
        return radius;
    }
}
