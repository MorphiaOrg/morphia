package dev.morphia.mapping.codec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.DateStorage;

import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonInt64;
import org.bson.BsonValue;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The date codecs read and write exactly what {@code Instant}/{@code ZonedDateTime} conversions in the storage zone
 * give, across negative epochs, millisecond boundaries and the extremes, for every {@link DateStorage}.
 */
public class TestDateCodecs {
    private static final List<Long> MILLIS = List.of(Long.MIN_VALUE / 1000, -86_400_001L, -86_400_000L, -86_399_999L, -1L, 0L,
            1L, 999L, 1_000L, 86_399_999L, 86_400_000L, 1_700_000_000_123L, 253_402_300_799_999L, Long.MAX_VALUE / 1000);

    @ParameterizedTest
    @EnumSource(DateStorage.class)
    void matchesZonedConversions(DateStorage storage) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                    .database("date_codecs")
                    .dateStorage(storage)
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false));
            Codec<LocalDateTime> dateTimes = datastore.getCodecRegistry().get(LocalDateTime.class);
            Codec<LocalDate> dates = datastore.getCodecRegistry().get(LocalDate.class);
            ZoneId zone = storage.getZone();

            for (long millis : MILLIS) {
                LocalDateTime expectedDateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime();
                Assertions.assertEquals(expectedDateTime, decode(dateTimes, new BsonDateTime(millis)), "date-time from " + millis);
                Assertions.assertEquals(expectedDateTime, decode(dateTimes, new BsonInt64(millis)), "date-time from INT64 " + millis);
                Assertions.assertEquals(new BsonDateTime(expectedDateTime.atZone(zone).toInstant().toEpochMilli()),
                        encode(dateTimes, expectedDateTime), "date-time to " + expectedDateTime);

                LocalDate expectedDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
                Assertions.assertEquals(expectedDate, decode(dates, new BsonDateTime(millis)), "date from " + millis);
                Assertions.assertEquals(new BsonDateTime(expectedDate.atStartOfDay(zone).toInstant().toEpochMilli()),
                        encode(dates, expectedDate), "date to " + expectedDate);
            }
            // values whose epoch milliseconds don't fit in a long fail to encode, as they always have, rather than wrapping
            for (LocalDateTime value : List.of(LocalDateTime.of(292278994, 8, 17, 7, 12, 56),
                    LocalDateTime.of(292278995, 1, 1, 0, 0), LocalDateTime.of(-292275056, 1, 1, 0, 0))) {
                Assertions.assertThrows(ArithmeticException.class, () -> value.atZone(zone).toInstant().toEpochMilli(),
                        "the reference conversion overflows for " + value);
                Assertions.assertThrows(ArithmeticException.class, () -> encode(dateTimes, value), "date-time " + value);
            }
            for (LocalDate value : List.of(LocalDate.of(292278994, 8, 18), LocalDate.of(292278995, 1, 1), LocalDate.of(-292275056, 1, 1))) {
                Assertions.assertThrows(ArithmeticException.class, () -> value.atStartOfDay(zone).toInstant().toEpochMilli(),
                        "the reference conversion overflows for " + value);
                Assertions.assertThrows(ArithmeticException.class, () -> encode(dates, value), "date " + value);
            }
            // the earliest and latest representable instants still encode
            for (long millis : List.of(Long.MIN_VALUE, Long.MAX_VALUE)) {
                LocalDateTime value = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime();
                Assertions.assertEquals(new BsonDateTime(millis), encode(dateTimes, value), "date-time at " + millis);
            }
            // sub-millisecond precision is dropped the same way, including before the epoch
            for (LocalDateTime value : List.of(LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999),
                    LocalDateTime.of(2024, 2, 29, 12, 0, 0, 1))) {
                Assertions.assertEquals(new BsonDateTime(value.atZone(zone).toInstant().toEpochMilli()), encode(dateTimes, value));
            }
        }
    }

    private static <T> T decode(Codec<T> codec, BsonValue value) {
        try (BsonDocumentReader reader = new BsonDocumentReader(new BsonDocument("v", value))) {
            reader.readStartDocument();
            reader.readBsonType();
            reader.readName();
            return codec.decode(reader, DecoderContext.builder().build());
        }
    }

    private static <T> BsonValue encode(Codec<T> codec, T value) {
        BsonDocument document = new BsonDocument();
        try (BsonDocumentWriter writer = new BsonDocumentWriter(document)) {
            writer.writeStartDocument();
            writer.writeName("v");
            codec.encode(writer, value, EncoderContext.builder().build());
            writer.writeEndDocument();
        }
        return document.get("v");
    }
}
