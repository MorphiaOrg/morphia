package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Geometry;
import dev.morphia.Datastore;
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
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(datastore.getMapper()));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getName());
        writer.writeName("$geometry");
        writeUnnamedValue(getValue(datastore), datastore, writer, context);
        writer.writeEndDocument();
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }
}
