package dev.morphia.query;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * Creates queries for GeoJson geo queries on MongoDB. These queries generally require MongoDB 2.4 and above, and usually work on 2d sphere
 * indexes.
 */
@SuppressWarnings("removal")
final class Geo2dSphereCriteria extends FieldCriteria {
    private final Geometry geometry;
    private Document options;
    private CoordinateReferenceSystem crs;

    private Geo2dSphereCriteria(final Mapper mapper, final String field, final dev.morphia.query.FilterOperator operator,
                                final Geometry geometry, final MappedClass mappedClass, final boolean validating) {
        super(mapper, field, operator, geometry, mappedClass, validating);
        this.geometry = geometry;
    }

    static Geo2dSphereCriteria geo(final Mapper mapper, final String field, final dev.morphia.query.FilterOperator operator,
                                   final Geometry value, final MappedClass mappedClass, final boolean validating) {
        return new Geo2dSphereCriteria(mapper, field, operator, value, mappedClass, validating);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Document toDocument() {
        Document query;
        dev.morphia.query.FilterOperator operator = getOperator();
        DocumentWriter writer = new DocumentWriter();
        ((Codec) getMapper().getCodecRegistry().get(geometry.getClass()))
            .encode(writer, geometry, EncoderContext.builder().build());
        Document document = new Document("$geometry", writer.getDocument());

        if (operator == dev.morphia.query.FilterOperator.NEAR || operator == dev.morphia.query.FilterOperator.NEAR_SPHERE) {
            if (options != null) {
                document.putAll(options);
            }
            query = new Document(dev.morphia.query.FilterOperator.NEAR.val(), document);
        } else if (operator == dev.morphia.query.FilterOperator.GEO_WITHIN || operator == dev.morphia.query.FilterOperator.INTERSECTS) {
            query = new Document(operator.val(), document);
            if (crs != null) {
                ((Document) document.get("$geometry")).put("crs", crs);
            }
        } else {
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
