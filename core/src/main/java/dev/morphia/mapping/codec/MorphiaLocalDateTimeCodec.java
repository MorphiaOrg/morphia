package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
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

    private final Datastore datastore;

    MorphiaLocalDateTimeCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public LocalDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType().equals(BsonType.INT64)) {
            return ofEpochMilli(reader.readInt64()).atZone(datastore.getMapper().getOptions().getDateStorage().getZone()).toLocalDateTime();
        }
        return ofEpochMilli(reader.readDateTime()).atZone(datastore.getMapper().getOptions().getDateStorage().getZone()).toLocalDateTime();
    }

    @Override
    public void encode(BsonWriter writer, LocalDateTime value, EncoderContext encoderContext) {
        writer.writeDateTime(value.atZone(datastore.getMapper().getOptions().getDateStorage().getZone()).toInstant().toEpochMilli());
    }

    @Override
    public Class<LocalDateTime> getEncoderClass() {
        return LocalDateTime.class;
    }
}
