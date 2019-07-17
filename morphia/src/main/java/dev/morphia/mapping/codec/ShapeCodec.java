package dev.morphia.mapping.codec;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Shape;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

class ShapeCodec implements Codec<Shape> {
    @Override
    public Shape decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException("Shape is intended for querying only and should not be used on entities.");
    }

    @Override
    public void encode(final BsonWriter writer, final Shape value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(value.getGeometry());
        for (final Point point : value.getPoints()) {
            encodePosition(writer, point.getCoordinates());
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    private void encodePosition(final BsonWriter writer, final Position value) {
        writer.writeStartArray();

        for (double number : value.getValues()) {
            writer.writeDouble(number);
        }

        writer.writeEndArray();
    }

    @Override
    public Class<Shape> getEncoderClass() {
        return Shape.class;
    }
}
