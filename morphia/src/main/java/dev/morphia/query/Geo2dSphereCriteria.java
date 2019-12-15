package dev.morphia.query;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.query.FilterOperator.NEAR;

/**
 * Creates queries for GeoJson geo queries on MongoDB. These queries generally require MongoDB 2.4 and above, and usually work on 2d sphere
 * indexes.
 */
final class Geo2dSphereCriteria extends FieldCriteria {
    private final Geometry geometry;
    private Document options;
    private CoordinateReferenceSystem crs;

    private Geo2dSphereCriteria(final Mapper mapper, final QueryImpl<?> query, final String field, final FilterOperator operator,
                                final Geometry geometry) {
        super(mapper, query, field, operator, geometry);
        this.geometry = geometry;
    }

    static Geo2dSphereCriteria geo(final Mapper mapper, final QueryImpl<?> query, final String field, final FilterOperator operator,
                                   final Geometry value) {
        return new Geo2dSphereCriteria(mapper, query, field, operator, value);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Document toDocument() {
        Document query;
        FilterOperator operator = getOperator();
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry().get(geometry.getClass()))
            .encode(writer, geometry, EncoderContext.builder().build());
        Document document = new Document("$geometry", writer.<Document>getRoot());

        switch (operator) {
            case NEAR:
            case NEAR_SPHERE:
                if (options != null) {
                    document.putAll(options);
                }
                query = new Document(NEAR.val(), document);
                break;
            case GEO_WITHIN:
            case INTERSECTS:
                query = new Document(operator.val(), document);
                if (crs != null) {
                    ((Document) document.get("$geometry")).put("crs", crs);
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Operator %s not supported for geo-query", operator.val()));
        }

        return new Document(getField(), query);
    }

    Geo2dSphereCriteria addCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    Geo2dSphereCriteria maxDistance(final Double maxDistance) {
        return manageOption("$maxDistance", maxDistance);
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

    Geo2dSphereCriteria minDistance(final Double minDistance) {
        return manageOption("$minDistance", minDistance);
    }
}
