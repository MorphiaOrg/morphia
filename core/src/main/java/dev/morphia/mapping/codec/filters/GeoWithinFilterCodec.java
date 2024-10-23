package dev.morphia.mapping.codec.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.GeoJsonObjectType;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Polygon;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.GeoWithinFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class GeoWithinFilterCodec extends BaseFilterCodec<GeoWithinFilter> {
    public GeoWithinFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, GeoWithinFilter value, EncoderContext encoderContext) {
        document(writer, value.path(datastore.getMapper()), () -> {
            if (value.isNot()) {
                document(writer, "$not", () -> {
                    encodeFilter(writer, value, encoderContext);
                });
            } else {
                encodeFilter(writer, value, encoderContext);
            }
        });
    }

    private void encodeFilter(BsonWriter writer, GeoWithinFilter value, EncoderContext encoderContext) {
        document(writer, value.getName(), () -> {
            document(writer, "$geometry", () -> {
                Geometry geometry = (Geometry) value.getValue();
                value(writer, "type", geometry.getType().getTypeName());
                GeoJsonObjectType type = geometry.getType();
                if (type == GeoJsonObjectType.POLYGON) {
                    var coordinates = ((Polygon) geometry).getCoordinates();
                    value(datastore.getCodecRegistry(), writer, "coordinates", coordinates, encoderContext);
                } else if (type == GeoJsonObjectType.MULTI_POLYGON) {
                    var coordinates = ((MultiPolygon) geometry).getCoordinates();
                    value(datastore.getCodecRegistry(), writer, "coordinates", coordinates, encoderContext);
                }
                CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
                if (crs != null) {
                    value(datastore.getCodecRegistry(), writer, "crs", crs, encoderContext);
                }

            });
        });
    }

    @Override
    public Class<GeoWithinFilter> getEncoderClass() {
        return GeoWithinFilter.class;
    }
}
