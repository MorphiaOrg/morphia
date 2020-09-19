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
    public dev.morphia.query.Shape decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(BsonWriter writer, dev.morphia.query.Shape value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(value.getGeometry());
        for (Point point : value.getPoints()) {
            encodePosition(writer, point.getCoordinates());
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    private void encodePosition(BsonWriter writer, Position value) {
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
