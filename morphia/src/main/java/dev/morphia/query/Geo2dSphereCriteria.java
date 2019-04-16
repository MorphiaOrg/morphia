package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.geo.CoordinateReferenceSystem;
import dev.morphia.geo.Geometry;
import dev.morphia.geo.GeometryQueryConverter;
import dev.morphia.geo.NamedCoordinateReferenceSystemConverter;
import org.bson.Document;

import static dev.morphia.query.FilterOperator.NEAR;

/**
 * Creates queries for GeoJson geo queries on MongoDB. These queries generally require MongoDB 2.4 and above, and usually work on 2d sphere
 * indexes.
 */
final class Geo2dSphereCriteria extends FieldCriteria {
    private Document options;
    private final Geometry geometry;
    private CoordinateReferenceSystem crs;

    private Geo2dSphereCriteria(final QueryImpl<?> query, final String field, final FilterOperator operator,
                                final Geometry geometry) {
        super(query, field, operator, geometry);
        this.geometry = geometry;
    }

    static Geo2dSphereCriteria geo(final QueryImpl<?> query, final String field, final FilterOperator operator,
                                   final Geometry value) {
        return new Geo2dSphereCriteria(query, field, operator, value);
    }

    Geo2dSphereCriteria maxDistance(final Double maxDistance) {
        return manageOption("$maxDistance", maxDistance);
    }

    Geo2dSphereCriteria minDistance(final Double minDistance) {
        return manageOption("$minDistance", minDistance);
    }

    private Geo2dSphereCriteria manageOption(final String key, final Object value) {
        if (options == null) {
            options = new Document();
        }
        if (value == null) {
            options.remove(key);
        } else {
            options.put(key, value);
        }

        return this;
    }

    Geo2dSphereCriteria addCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    @Override
    public DBObject toDBObject() {
        DBObject query;
        FilterOperator operator = getOperator();
        GeometryQueryConverter geometryQueryConverter = new GeometryQueryConverter(getQuery().getDatastore().getMapper());
        final DBObject geometryAsDBObject = (DBObject) geometryQueryConverter.encode(geometry, null);

        switch (operator) {
            case NEAR:
            case NEAR_SPHERE:
                if (options != null) {
                    geometryAsDBObject.putAll(options);
                }
                query = new BasicDBObject(NEAR.val(), geometryAsDBObject);
                break;
            case GEO_WITHIN:
            case INTERSECTS:
                query = new BasicDBObject(operator.val(), geometryAsDBObject);
                if (crs != null) {
                    ((DBObject) geometryAsDBObject.get("$geometry")).put("crs", new NamedCoordinateReferenceSystemConverter().encode(crs));
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Operator %s not supported for geo-query", operator.val()));
        }

        return new BasicDBObject(getField(), query);
    }
}
