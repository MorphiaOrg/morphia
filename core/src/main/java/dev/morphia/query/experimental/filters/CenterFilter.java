package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class CenterFilter extends Filter {
    private final double radius;

    protected CenterFilter(String filterName, String field, Point value, double radius) {
        super(filterName, field, value);
        this.radius = radius;
    }

    @Override
    protected Point getValue() {
        return (Point) super.getValue();
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(mapper));
        writer.writeStartDocument("$geoWithin");

        writer.writeStartArray(getFilterName());
        Point center = getValue();
        writer.writeStartArray();
        for (Double value : center.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeDouble(radius);
        writer.writeEndArray();

        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
