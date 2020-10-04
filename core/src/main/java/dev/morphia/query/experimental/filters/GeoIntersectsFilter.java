package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 */
public class GeoIntersectsFilter extends Filter {
    GeoIntersectsFilter(String field, Geometry val) {
        super("$geoIntersects", field, val);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(mapper));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getFilterName());
        writer.writeName("$geometry");
        writeUnnamedValue(getValue(mapper), mapper, writer, context);
        writer.writeEndDocument();
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }
}
