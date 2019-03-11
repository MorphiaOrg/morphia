package dev.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import java.util.Map;

/**
 * Geospatial specific FieldCriteria logic
 */
class Geo2dCriteria extends FieldCriteria {

    private final Map<String, Object> opts;

    Geo2dCriteria(final QueryImpl<?> query, final String field, final FilterOperator op, final Object value,
                  final Map<String, Object> opts) {
        super(query, field, op, value);
        this.opts = opts;
    }

    @Override
    public DBObject toDBObject() {
        final DBObject obj = new BasicDBObject();
        final BasicDBObjectBuilder query;
        switch (getOperator()) {
            case NEAR:
                query = BasicDBObjectBuilder.start(FilterOperator.NEAR.val(), getValue());
                break;
            case NEAR_SPHERE:
                query = BasicDBObjectBuilder.start(FilterOperator.NEAR_SPHERE.val(), getValue());
                break;
            case WITHIN_BOX:
                query = BasicDBObjectBuilder.start().push(FilterOperator.GEO_WITHIN.val()).add(getOperator().val(), getValue());
                break;
            case WITHIN_CIRCLE:
                query = BasicDBObjectBuilder.start().push(FilterOperator.GEO_WITHIN.val()).add(getOperator().val(), getValue());
                break;
            case WITHIN_CIRCLE_SPHERE:
                query = BasicDBObjectBuilder.start().push(FilterOperator.GEO_WITHIN.val()).add(getOperator().val(), getValue());
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

        obj.put(getField(), query.get());

        return obj;
    }
}
