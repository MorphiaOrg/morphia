package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import org.bson.BsonWriter;

class PolygonFilter extends GeoWithinFilter {
    private final Point[] points;

    public PolygonFilter(final String field, final Point[] points) {
        super("$polygon", field, null);
        this.points = points;
    }

    @Override
    protected void encodeShape(final BsonWriter writer) {
        writer.writeStartArray("$polygon");
        for (final Point point : points) {
            writer.writeStartArray();
            for (final Double value : point.getPosition().getValues()) {
                writer.writeDouble(value);
            }
            writer.writeEndArray();
        }
        writer.writeEndArray();
    }
}
