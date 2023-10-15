package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Polygon;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines a $geoWithin filter.
 *
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GeoWithinFilter extends Filter {
    private CoordinateReferenceSystem crs;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    GeoWithinFilter(String field, Polygon value) {
        super("$geoWithin", field, value);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    GeoWithinFilter(String field, MultiPolygon value) {
        super("$geoWithin", field, value);
    }

    /**
     * @param crs the CoordinateReferenceSystem to use
     * @return this
     */
    public GeoWithinFilter crs(CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

}
