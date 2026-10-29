package dev.morphia.mapping.codec;

import java.time.Instant;
import java.time.LocalDate;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.DateStorage;

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
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private MorphiaDatastore datastore;
    // UTC storage converts with plain day arithmetic; the system default zone is looked up per value, since it can
    // change while running
    private final boolean utc;

    MorphiaDateCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
        utc = datastore.getMapper().getConfig().dateStorage() == DateStorage.UTC;
    }

    @Override
    public LocalDate decode(BsonReader reader, DecoderContext decoderContext) {
        long millis = reader.readDateTime();
        if (utc) {
            return LocalDate.ofEpochDay(Math.floorDiv(millis, MILLIS_PER_DAY));
        }
        return Instant.ofEpochMilli(millis)
                .atZone(datastore.getMapper().getConfig().dateStorage().getZone())
                .toLocalDate();
    }

    @Override
    public void encode(BsonWriter writer, LocalDate value, EncoderContext encoderContext) {
        if (utc) {
            // a date whose midnight doesn't fit in a long of milliseconds throws rather than wrapping
            writer.writeDateTime(Math.multiplyExact(value.toEpochDay(), MILLIS_PER_DAY));
        } else {
            writer.writeDateTime(value.atStartOfDay(datastore.getMapper().getConfig().dateStorage().getZone()).toInstant().toEpochMilli());
        }
    }

    @Override
    public Class<LocalDate> getEncoderClass() {
        return LocalDate.class;
    }
}
