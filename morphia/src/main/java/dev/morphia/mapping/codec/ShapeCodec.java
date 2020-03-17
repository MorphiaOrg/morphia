package dev.morphia.mapping.codec;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

@SuppressWarnings("removal")
class ShapeCodec implements Codec<dev.morphia.query.Shape> {
    @Override
    public dev.morphia.query.Shape decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(final BsonWriter writer, final dev.morphia.query.Shape value, final EncoderContext encoderContext) {
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
    public Class<dev.morphia.query.Shape> getEncoderClass() {
        return dev.morphia.query.Shape.class;
    }
}
