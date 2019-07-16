package dev.morphia.query;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.geo.GeometryQueryConverter;
import dev.morphia.geo.NamedCoordinateReferenceSystemConverter;
import dev.morphia.mapping.Mapper;
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

    private Geo2dSphereCriteria(final Mapper mapper, final QueryImpl<?> query, final String field, final FilterOperator operator,
                                final Geometry geometry) {
        super(mapper, query, field, operator, geometry);
        this.geometry = geometry;
    }

    static Geo2dSphereCriteria geo(final Mapper mapper, final QueryImpl<?> query, final String field, final FilterOperator operator,
                                   final com.mongodb.client.model.geojson.Geometry value) {
        return new Geo2dSphereCriteria(mapper, query, field, operator, value);
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
    public Document toDocument() {
        if (1 == 1) {
            //TODO:  implement this
            throw new UnsupportedOperationException("the codecs should take care of all this");
        }

        Document query;
        FilterOperator operator = getOperator();
        GeometryQueryConverter geometryQueryConverter = new GeometryQueryConverter(getMapper());
        final Document geometryAsDBObject = (Document) geometryQueryConverter.encode(geometry, null);

        switch (operator) {
            case NEAR:
            case NEAR_SPHERE:
                if (options != null) {
                    geometryAsDBObject.putAll(options);
                }
                query = new Document(NEAR.val(), geometryAsDBObject);
                break;
            case GEO_WITHIN:
            case INTERSECTS:
                query = new Document(operator.val(), geometryAsDBObject);
                if (crs != null) {
                    ((Document) geometryAsDBObject.get("$geometry")).put("crs", new NamedCoordinateReferenceSystemConverter().encode(crs,
                     null));
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Operator %s not supported for geo-query", operator.val()));
        }

        return new Document(getField(), query);
    }
}
