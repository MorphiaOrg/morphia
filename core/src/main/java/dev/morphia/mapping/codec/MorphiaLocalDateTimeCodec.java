package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.LocalDateTime;

import static java.time.Instant.ofEpochMilli;

/**
 * Converts the {@code LocalDateTime} values to and from the zone defined in {@link MapperOptions#getDateStorage()}
 *
 * @since 2.0
 */
public class MorphiaLocalDateTimeCodec implements Codec<LocalDateTime> {

    private final Mapper mapper;

    MorphiaLocalDateTimeCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LocalDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType().equals(BsonType.INT64)) {
            return ofEpochMilli(reader.readInt64()).atZone(mapper.getOptions().getDateStorage().getZone()).toLocalDateTime();
        }
        return ofEpochMilli(reader.readDateTime()).atZone(mapper.getOptions().getDateStorage().getZone()).toLocalDateTime();
    }

    @Override
    public void encode(BsonWriter writer, LocalDateTime value, EncoderContext encoderContext) {
        writer.writeDateTime(value.atZone(mapper.getOptions().getDateStorage().getZone()).toInstant().toEpochMilli());
    }

    @Override
    public Class<LocalDateTime> getEncoderClass() {
        return LocalDateTime.class;
    }
}
