package dev.morphia.query;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.lang.Nullable;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
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

    private Geo2dSphereCriteria(Mapper mapper, String field, dev.morphia.query.FilterOperator operator,
                                Geometry geometry, EntityModel model, boolean validating) {
        super(mapper, field, operator, geometry, model, validating);
        this.geometry = geometry;
    }

    static Geo2dSphereCriteria geo(Mapper mapper, String field, dev.morphia.query.FilterOperator operator,
                                   Geometry value, EntityModel model, boolean validating) {
        return new Geo2dSphereCriteria(mapper, field, operator, value, model, validating);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Document toDocument() {
        Document query;
        dev.morphia.query.FilterOperator operator = getOperator();
        DocumentWriter writer = new DocumentWriter(getMapper());
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

    Geo2dSphereCriteria addCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    private Geo2dSphereCriteria manageOption(String key, @Nullable Object value) {
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

    Geo2dSphereCriteria maxDistance(@Nullable Double maxDistance) {
        return manageOption("$maxDistance", maxDistance);
    }

    Geo2dSphereCriteria minDistance(@Nullable Double minDistance) {
        return manageOption("$minDistance", minDistance);
    }
}
