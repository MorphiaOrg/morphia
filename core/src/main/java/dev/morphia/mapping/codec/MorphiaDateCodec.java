package dev.morphia.mapping.codec;

import java.time.Instant;
import java.time.LocalDate;

import dev.morphia.config.MorphiaConfig;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import static dev.morphia.internal.DatastoreHolder.holder;

/**
 * Converts the {@code LocalDate} values to and from the zone defined in {@link MorphiaConfig#dateStorage()}
 *
 * @since 2.0
 */
public class MorphiaDateCodec implements Codec<LocalDate> {

    MorphiaDateCodec() {
    }

    @Override
    public LocalDate decode(BsonReader reader, DecoderContext decoderContext) {
        return Instant.ofEpochMilli(reader.readDateTime())
                .atZone(holder.get().getMapper().getConfig().dateStorage().getZone())
                .toLocalDate();
    }

    @Override
    public void encode(BsonWriter writer, LocalDate value, EncoderContext encoderContext) {
        writer.writeDateTime(value.atStartOfDay(holder.get().getMapper().getConfig().dateStorage().getZone()).toInstant().toEpochMilli());
    }

    @Override
    public Class<LocalDate> getEncoderClass() {
        return LocalDate.class;
    }
}
