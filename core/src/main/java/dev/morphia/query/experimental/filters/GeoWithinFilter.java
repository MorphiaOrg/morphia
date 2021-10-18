package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Polygon;
import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * Defines a $geoWithin filter.
 *
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GeoWithinFilter extends Filter {
    private CoordinateReferenceSystem crs;

    GeoWithinFilter(String field, Polygon value) {
        super("$geoWithin", field, value);
    }

    GeoWithinFilter(String field, MultiPolygon value) {
        super("$geoWithin", field, value);
    }

    /**
     * @param crs the CoordinateReferenceSystem to use
     * @return this
     */
    public GeoWithinFilter crs(CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    @Override
    public final void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(datastore.getMapper()));
        writer.writeStartDocument(getName());
        writer.writeName("$geometry");

        Object shape = getValue();
        if (shape != null) {
            Codec codec = datastore.getCodecRegistry().get(shape.getClass());
            codec.encode(writer, shape, context);
        }

        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
