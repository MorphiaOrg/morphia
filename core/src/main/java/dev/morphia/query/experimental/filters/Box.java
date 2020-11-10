package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class Box extends Filter {

    private final Point bottomLeft;
    private final Point upperRight;

    protected Box(String field, Point bottomLeft, Point upperRight) {
        super("$box", field, null);
        this.bottomLeft = bottomLeft;
        this.upperRight = upperRight;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        writer.writeStartDocument(path(mapper));
        writer.writeStartDocument("$geoWithin");

        writer.writeStartArray(getName());
        writer.writeStartArray();
        for (Double value : bottomLeft.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeStartArray();
        for (Double value : upperRight.getPosition().getValues()) {
            writer.writeDouble(value);
        }
        writer.writeEndArray();
        writer.writeEndArray();

        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
