package dev.morphia.query;


import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.Map;

/**
 * Geospatial specific FieldCriteria logic
 */
class Geo2dCriteria extends FieldCriteria {

    private final Map<String, Object> opts;

    Geo2dCriteria(final Mapper mapper, final String field, final FilterOperator op, final Object value,
                  final Map<String, Object> opts, final MappedClass mappedClass, final boolean validating) {
        super(mapper, field, op, value, mappedClass, validating);
        this.opts = opts;
    }

    @Override
    public Document toDocument() {
        final Document obj = new Document();
        final Document query;
        switch (getOperator()) {
            case NEAR:
                query = new Document(FilterOperator.NEAR.val(), getValue());
                break;
            case NEAR_SPHERE:
                query = new Document(FilterOperator.NEAR_SPHERE.val(), getValue());
                break;
            case WITHIN_BOX:
            case WITHIN_CIRCLE:
            case WITHIN_CIRCLE_SPHERE:
                query = new Document(FilterOperator.GEO_WITHIN.val(), new Document(getOperator().val(), getValue()));
                break;
            default:
                throw new UnsupportedOperationException(getOperator() + " not supported for geo-query");
        }

        //add options...
        if (opts != null) {
            for (final Map.Entry<String, Object> e : opts.entrySet()) {
                query.append(e.getKey(), e.getValue());
            }
        }

        obj.put(getField(), query);

        return obj;
    }
}
