package dev.morphia.query;


import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import org.bson.Document;

import java.util.Map;

/**
 * Geospatial specific FieldCriteria logic
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
class Geo2dCriteria extends FieldCriteria {

    private final Map<String, Object> opts;

    Geo2dCriteria(Mapper mapper, String field, FilterOperator op, Object value,
                  Map<String, Object> opts, EntityModel model, boolean validating) {
        super(mapper, field, op, value, model, validating);
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
            for (Map.Entry<String, Object> e : opts.entrySet()) {
                query.append(e.getKey(), e.getValue());
            }
        }

        obj.put(getField(), query);

        return obj;
    }
}
