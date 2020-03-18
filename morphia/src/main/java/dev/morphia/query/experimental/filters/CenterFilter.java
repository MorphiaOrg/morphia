package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class CenterFilter extends Filter {
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
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        writer.writeStartDocument("$geoWithin");

        writer.writeStartArray(getFilterName());
        Point center = getValue();
        writer.writeStartArray();
        for (final Double value : center.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeDouble(radius);
        writer.writeEndArray();

        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
