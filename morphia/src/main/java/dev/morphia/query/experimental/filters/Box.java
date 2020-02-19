package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import org.bson.BsonWriter;

class Box extends GeoWithinFilter {

    private final Point bottomLeft;
    private final Point upperRight;

    protected Box(final String field, final Point bottomLeft, final Point upperRight) {
        super("$box", field, null);
        this.bottomLeft = bottomLeft;
        this.upperRight = upperRight;
    }

    @Override
    protected void encodeShape(final BsonWriter writer) {
        writer.writeStartArray("$box");
        writer.writeStartArray();
        for (final Double value : bottomLeft.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeStartArray();
        for (final Double value : upperRight.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeEndArray();
    }
}
