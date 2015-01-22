package org.mongodb.morphia.query;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.mongodb.morphia.geo.Geometry;
import org.mongodb.morphia.geo.GeometryQueryConverter;

import static org.mongodb.morphia.query.FilterOperator.NEAR;

/**
 * Creates queries for GeoJson geo queries on MongoDB. These queries generally require MongoDB 2.4 and above, and usually work on 2dsphere
 * indexes.
 */
class StandardGeoFieldCriteria extends FieldCriteria {
    private final Integer maxDistanceMeters;
    private final DBObject geometryAsDBObject;

    protected StandardGeoFieldCriteria(final QueryImpl<?> query, final String field, final FilterOperator operator, final Geometry value,
                                       final Integer maxDistanceMeters, final boolean validateNames, final boolean validateTypes) {
        super(query, field, operator, value, validateNames, validateTypes);
        this.maxDistanceMeters = maxDistanceMeters;
        GeometryQueryConverter geometryQueryConverter = new GeometryQueryConverter(query.getDatastore().getMapper());
        geometryAsDBObject = (DBObject) geometryQueryConverter.encode(value, null);
    }

    @Override
    public void addTo(final DBObject obj) {
        BasicDBObjectBuilder query;
        FilterOperator operator = getOperator();

        switch (operator) {
            case NEAR:
                if (maxDistanceMeters != null) {
                    geometryAsDBObject.put("$maxDistance", maxDistanceMeters);
                }
                query = BasicDBObjectBuilder.start(NEAR.val(), geometryAsDBObject);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Operator %s not supported for geo-query", operator.val()));
        }

        obj.put(getField(), query.get());
    }
}
