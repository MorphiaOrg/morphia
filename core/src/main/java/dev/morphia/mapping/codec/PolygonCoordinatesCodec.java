package dev.morphia.mapping.codec;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import static java.lang.String.format;

/**
 * This codec has been cobbled together via sources from com.mongodb.client.model.geojson.codecs.GeometryDecoderHelper
 * and com.mongodb.client.model.geojson.codecs.GeometryEncoderHelper
 */
public class PolygonCoordinatesCodec implements Codec<PolygonCoordinates> {

    @Override
    public PolygonCoordinates decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartArray();
        List<List<Position>> values = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            values.add(decodeCoordinates(reader));
        }
        reader.readEndArray();

        if (values.isEmpty()) {
            throw new CodecConfigurationException("Invalid Polygon no coordinates.");
        }

        List<Position> exterior = values.remove(0);

        try {
            return new PolygonCoordinates(exterior, values);
        } catch (IllegalArgumentException e) {
            throw new CodecConfigurationException(format("Invalid Polygon: %s", e.getMessage()));
        }

    }

    @Override
    public void encode(BsonWriter writer, PolygonCoordinates polygonCoordinates, EncoderContext encoderContext) {
        writer.writeStartArray();
        encodeLinearRing(polygonCoordinates.getExterior(), writer);
        for (List<Position> ring : polygonCoordinates.getHoles()) {
            encodeLinearRing(ring, writer);
        }
        writer.writeEndArray();
    }

    @Override
    public Class<PolygonCoordinates> getEncoderClass() {
        return PolygonCoordinates.class;
    }

    private static List<Position> decodeCoordinates(final BsonReader reader) {
        validateIsArray(reader);
        reader.readStartArray();
        List<Position> values = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            values.add(decodePosition(reader));
        }
        reader.readEndArray();
        return values;
    }

    private static Position decodePosition(final BsonReader reader) {
        validateIsArray(reader);
        reader.readStartArray();
        List<Double> values = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            values.add(readAsDouble(reader));
        }
        reader.readEndArray();

        try {
            return new Position(values);
        } catch (IllegalArgumentException e) {
            throw new CodecConfigurationException(format("Invalid Position: %s", e.getMessage()));
        }
    }

    private static double readAsDouble(final BsonReader reader) {
        if (reader.getCurrentBsonType() == BsonType.DOUBLE) {
            return reader.readDouble();
        } else if (reader.getCurrentBsonType() == BsonType.INT32) {
            return reader.readInt32();
        } else if (reader.getCurrentBsonType() == BsonType.INT64) {
            return reader.readInt64();
        }

        throw new CodecConfigurationException("A GeoJSON position value must be a numerical type, but the value is of type "
                + reader.getCurrentBsonType());
    }

    private void encodeLinearRing(final List<Position> ring, final BsonWriter writer) {
        writer.writeStartArray();
        for (Position position : ring) {
            encodePosition(writer, position);
        }
        writer.writeEndArray();
    }

    private void encodePosition(final BsonWriter writer, final Position value) {
        writer.writeStartArray();

        for (double number : value.getValues()) {
            writer.writeDouble(number);
        }

        writer.writeEndArray();
    }

    private static void validateIsArray(final BsonReader reader) {
        if (reader.getCurrentBsonType() != BsonType.ARRAY) {
            throw new CodecConfigurationException("Invalid BsonType expecting an Array");
        }
    }

}
