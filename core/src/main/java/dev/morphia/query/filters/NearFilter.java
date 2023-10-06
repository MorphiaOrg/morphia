package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines a filter for $near and $nearSphere queries
 *
 * @since 2.0
 */
public class NearFilter extends Filter {
    private Double maxDistance;
    private Double minDistance;
    private CoordinateReferenceSystem crs;

    /**
     * @param filterName the filter name
     * @param field      the field
     * @param point      the point
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    NearFilter(String filterName, String field, Point point) {
        super(filterName, field, point);
    }

    /**
     * Sets the max distance to consider
     *
     * @param maxDistance the max
     * @return this
     */
    public NearFilter maxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /**
     * Sets the min distance to consider
     *
     * @param minDistance the min
     * @return this
     */
    public NearFilter minDistance(Double minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    /**
     * Sets the coordinate reference system to use
     *
     * @param crs the crs
     * @return this
     */
    public NearFilter crs(CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the max distance
     */
    @MorphiaInternal
    public Double maxDistance() {
        return maxDistance;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the min distance
     */
    @MorphiaInternal
    public Double minDistance() {
        return minDistance;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the reference system
     */
    @Nullable
    @MorphiaInternal
    public CoordinateReferenceSystem crs() {
        return crs;
    }
}
