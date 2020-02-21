package dev.morphia.mapping.codec;

import com.mongodb.client.model.geojson.Position;
import dev.morphia.query.Shape;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

class CenterCodec implements Codec<Shape.Center> {
    @Override
    public Shape.Center decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(final BsonWriter writer, final Shape.Center value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(value.getGeometry());

        encodePosition(writer, value.getCenter().getPosition());
        writer.writeDouble(value.getRadius());

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
    public Class<Shape.Center> getEncoderClass() {
        return Shape.Center.class;
    }
}
