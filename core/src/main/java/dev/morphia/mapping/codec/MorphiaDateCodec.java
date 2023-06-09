package dev.morphia.mapping.codec;

import java.time.Instant;
import java.time.LocalDate;

import dev.morphia.Datastore;
import dev.morphia.config.MorphiaConfig;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Converts the {@code LocalDate} values to and from the zone defined in {@link MorphiaConfig#dateStorage()}
 *
 * @since 2.0
 */
public class MorphiaDateCodec implements Codec<LocalDate> {

    private final Datastore datastore;

    MorphiaDateCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public LocalDate decode(BsonReader reader, DecoderContext decoderContext) {
        return Instant.ofEpochMilli(reader.readDateTime())
                .atZone(datastore.getMapper().getConfig().dateStorage().getZone())
                .toLocalDate();
    }

    @Override
    public void encode(BsonWriter writer, LocalDate value, EncoderContext encoderContext) {
        writer.writeDateTime(value.atStartOfDay(datastore.getMapper().getConfig().dateStorage().getZone()).toInstant().toEpochMilli());
    }

    @Override
    public Class<LocalDate> getEncoderClass() {
        return LocalDate.class;
    }
}
