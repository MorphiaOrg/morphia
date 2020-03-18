package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class PolygonFilter extends Filter {
    private final Point[] points;

    PolygonFilter(final String field, final Point[] points) {
        super("$polygon", field, null);
        this.points = points;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        writer.writeStartDocument("$geoWithin");
        writer.writeStartArray("$polygon");
        for (final Point point : points) {
            writer.writeStartArray();
            for (final Double value : point.getPosition().getValues()) {
                writer.writeDouble(value);
            }
            writer.writeEndArray();
        }
        writer.writeEndArray();
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
