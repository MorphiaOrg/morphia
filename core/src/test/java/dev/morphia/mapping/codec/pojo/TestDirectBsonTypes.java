package dev.morphia.mapping.codec.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.codec.EnumCodec;
import dev.morphia.mapping.codec.MorphiaDateCodec;
import dev.morphia.mapping.codec.MorphiaLocalDateTimeCodec;

import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonSymbol;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.codecs.BigDecimalCodec;
import org.bson.codecs.BooleanCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DateCodec;
import org.bson.codecs.Decimal128Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DoubleCodec;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.LongCodec;
import org.bson.codecs.ObjectIdCodec;
import org.bson.codecs.StringCodec;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;

/**
 * Pins the assumption behind {@link CodecPlan}'s direct-type table against the codecs the driver and Morphia actually
 * provide: for every BSON type a codec is listed as reading directly, it reads the whole value without throwing, so
 * decoding that type needs no reader mark. A driver upgrade that changes one of these codecs fails here.
 */
public class TestDirectBsonTypes {
    // the Java type whose codec is each table entry
    private static final Map<Class<?>, Class<?>> JAVA_TYPES = Map.ofEntries(
            entry(StringCodec.class, String.class),
            entry(ObjectIdCodec.class, ObjectId.class),
            entry(IntegerCodec.class, Integer.class),
            entry(LongCodec.class, Long.class),
            entry(DoubleCodec.class, Double.class),
            entry(BooleanCodec.class, Boolean.class),
            entry(Decimal128Codec.class, Decimal128.class),
            entry(BigDecimalCodec.class, BigDecimal.class),
            entry(DateCodec.class, Date.class),
            entry(EnumCodec.class, Color.class),
            entry(MorphiaLocalDateTimeCodec.class, LocalDateTime.class),
            entry(MorphiaDateCodec.class, LocalDate.class));

    // values of each BSON type, including the extremes a codec might reject after reading
    private static final Map<BsonType, List<BsonValue>> SAMPLES = Map.ofEntries(
            entry(BsonType.STRING, List.of(new BsonString(""), new BsonString("text"))),
            entry(BsonType.SYMBOL, List.of(new BsonSymbol("symbol"))),
            entry(BsonType.OBJECT_ID, List.of(new BsonObjectId(new ObjectId()))),
            entry(BsonType.INT32, List.of(new BsonInt32(Integer.MIN_VALUE), new BsonInt32(0), new BsonInt32(Integer.MAX_VALUE))),
            entry(BsonType.INT64, List.of(new BsonInt64(Long.MIN_VALUE), new BsonInt64(1_700_000_000_000L),
                    new BsonInt64(Long.MAX_VALUE))),
            entry(BsonType.DOUBLE, List.of(new BsonDouble(1.5), new BsonDouble(-0.0), new BsonDouble(Double.NaN),
                    new BsonDouble(Double.MAX_VALUE))),
            entry(BsonType.BOOLEAN, List.of(BsonBoolean.TRUE, BsonBoolean.FALSE)),
            entry(BsonType.DECIMAL128, List.of(new BsonDecimal128(Decimal128.parse("1.5")),
                    new BsonDecimal128(Decimal128.parse("-12345678901234567890.123")))),
            entry(BsonType.DATE_TIME, List.of(new BsonDateTime(0), new BsonDateTime(1_700_000_000_000L))));

    private static MongoClient client;
    private static MorphiaDatastore datastore;

    @BeforeAll
    static void datastore() {
        // nothing here talks to a server
        client = MongoClients.create("mongodb://localhost:1");
        datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                .database("direct_bson_types")
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false));
    }

    @AfterAll
    static void close() {
        client.close();
    }

    @Test
    void listedCodecsReadTheirTypesWithoutFailing() {
        List<String> checked = new ArrayList<>();
        CodecPlan.DIRECT_BSON_TYPES.forEach((codecClass, mask) -> {
            Class<?> javaType = JAVA_TYPES.get(codecClass);
            Assertions.assertNotNull(javaType, "no Java type to look up " + codecClass.getName() + " with");
            Codec<?> codec = datastore.getCodecRegistry().get(javaType);
            Assertions.assertSame(codecClass, codec.getClass(),
                    "the table lists " + codecClass.getName() + " but " + javaType.getName() + " resolves to " + codec.getClass());

            for (BsonType type : BsonType.values()) {
                if ((mask & (1 << type.ordinal())) == 0) {
                    continue;
                }
                List<BsonValue> samples = SAMPLES.get(type);
                Assertions.assertNotNull(samples, "no samples of " + type);
                for (BsonValue sample : samples) {
                    BsonValue value = codec instanceof EnumCodec ? new BsonString(Color.RED.name()) : sample;
                    assertReadsWhole(codec, value);
                    checked.add(codecClass.getSimpleName() + "/" + type);
                }
            }
        });
        Assertions.assertFalse(checked.isEmpty());
    }

    private static void assertReadsWhole(Codec<?> codec, BsonValue value) {
        try (BsonDocumentReader reader = new BsonDocumentReader(new BsonDocument("v", value))) {
            reader.readStartDocument();
            reader.readBsonType();
            reader.readName();
            try {
                codec.decode(reader, DecoderContext.builder().build());
            } catch (RuntimeException e) {
                Assertions.fail(codec.getClass().getSimpleName() + " should read " + value + " directly", e);
            }
            Assertions.assertEquals(BsonType.END_OF_DOCUMENT, reader.readBsonType(),
                    codec.getClass().getSimpleName() + " should consume all of " + value);
        }
    }

    enum Color {
        RED
    }
}
