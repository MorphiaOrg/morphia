package dev.morphia.query.filters;

import com.mongodb.client.model.geojson.Geometry;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class GeoIntersectsFilter extends Filter {
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    GeoIntersectsFilter(String field, Geometry val) {
        super("$geoIntersects", field, val);
    }
}
