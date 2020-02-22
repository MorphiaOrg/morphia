package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

abstract class GeoWithinFilter extends Filter {
    GeoWithinFilter(final String name, final String field, final Geometry value) {
        super(name, field, value);
    }

    @Override
    public final void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        writer.writeStartDocument("$geoWithin");
        encodeShape(writer);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    protected abstract void encodeShape(BsonWriter writer);
}
