package dev.morphia.test.mapping.codec;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MapperOptions;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.of;

/**
 * Encodes a ZonedDateTime to/from the database using the zone defined in {@link MapperOptions#getDateStorage()}
 *
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
public class ZonedDateTimeCodec implements Codec<ZonedDateTime> {
    public static final ZoneId UTC = of("UTC");

    @Override
    public ZonedDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType().equals(BsonType.INT64)) {
            return ofEpochMilli(reader.readInt64()).atZone(UTC);
        }
        return ofEpochMilli(reader.readDateTime()).atZone(UTC);
    }

    @Override
    public void encode(BsonWriter writer, ZonedDateTime value, EncoderContext encoderContext) {
        writer.writeDateTime(value.toInstant().toEpochMilli());
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
        return ZonedDateTime.class;
    }
}
