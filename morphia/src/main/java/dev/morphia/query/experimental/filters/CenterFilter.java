package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import org.bson.BsonWriter;

class CenterFilter extends GeoWithinFilter {
    private final double radius;

    protected CenterFilter(final String filterName, final String field, final Point value, final double radius) {
        super(filterName, field, value);
        this.radius = radius;
    }

    @Override
    protected Point getValue() {
        return (Point) super.getValue();
    }

    @Override
    protected void encodeShape(final BsonWriter writer) {
        writer.writeStartArray(getFilterName());
        Point center = getValue();
        writer.writeStartArray();
        for (final Double value : center.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeDouble(radius);
        writer.writeEndArray();
    }
}
