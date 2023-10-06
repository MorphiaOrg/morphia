package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.Point;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class Box extends Filter {

    private final Point bottomLeft;
    private final Point upperRight;

    /**
     * @param field      the field
     * @param bottomLeft the bottom left point
     * @param upperRight the upper right point
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Box(String field, Point bottomLeft, Point upperRight) {
        super("$box", field, null);
        this.bottomLeft = bottomLeft;
        this.upperRight = upperRight;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the bottom left
     */
    @MorphiaInternal
    public Point bottomLeft() {
        return bottomLeft;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the upper right
     */
    @MorphiaInternal
    public Point upperRight() {
        return upperRight;
    }
}
