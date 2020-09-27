package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class PolygonFilter extends Filter {
    private final Point[] points;

    PolygonFilter(String field, Point[] points) {
        super("$polygon", field, null);
        this.points = points;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        writer.writeStartDocument("$geoWithin");
        writer.writeStartArray("$polygon");
        for (Point point : points) {
            writer.writeStartArray();
            for (Double value : point.getPosition().getValues()) {
                writer.writeDouble(value);
            }
            writer.writeEndArray();
        }
        writer.writeEndArray();
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
