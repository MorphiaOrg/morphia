package dev.morphia.mapping.codec;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.DateStorage;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import static java.time.Instant.ofEpochMilli;

/**
 * Converts the {@code LocalDateTime} values to and from the zone defined in {@link MorphiaConfig#dateStorage()}
 *
 * @since 2.0
 */
public class MorphiaLocalDateTimeCodec implements Codec<LocalDateTime> {

    private MorphiaDatastore datastore;
    // UTC storage converts with plain offset arithmetic; the system default zone is looked up per value, since it can
    // change while running
    private final boolean utc;

    MorphiaLocalDateTimeCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
        utc = datastore.getMapper().getConfig().dateStorage() == DateStorage.UTC;
    }

    @Override
    public LocalDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        long millis = reader.getCurrentBsonType().equals(BsonType.INT64) ? reader.readInt64() : reader.readDateTime();
        if (utc) {
            return LocalDateTime.ofEpochSecond(Math.floorDiv(millis, 1000), (int) Math.floorMod(millis, 1000) * 1_000_000,
                    ZoneOffset.UTC);
        }
        return ofEpochMilli(millis).atZone(datastore.getMapper().getConfig().dateStorage().getZone()).toLocalDateTime();
    }

    @Override
    public void encode(BsonWriter writer, LocalDateTime value, EncoderContext encoderContext) {
        if (utc) {
            writer.writeDateTime(epochMillis(value.toEpochSecond(ZoneOffset.UTC), value.getNano()));
        } else {
            writer.writeDateTime(value.atZone(datastore.getMapper().getConfig().dateStorage().getZone()).toInstant().toEpochMilli());
        }
    }

    /**
     * The epoch milliseconds of an instant, computed as {@link java.time.Instant#toEpochMilli()} does: an instant whose
     * milliseconds don't fit in a long throws rather than wrapping, while the earliest that do still convert.
     */
    private static long epochMillis(long seconds, int nanos) {
        if (seconds < 0 && nanos > 0) {
            return Math.addExact(Math.multiplyExact(seconds + 1, 1000), nanos / 1_000_000 - 1000);
        }
        return Math.addExact(Math.multiplyExact(seconds, 1000), nanos / 1_000_000);
    }

    @Override
    public Class<LocalDateTime> getEncoderClass() {
        return LocalDateTime.class;
    }
}
