package dev.morphia.mapping.codec.pojo;

import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.test.mapping.codec.harness.models.HarnessReferences;
import dev.morphia.test.mapping.codec.harness.models.HarnessScalars;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * A refresh codec decodes with the entity's regular plan when there is one, and builds its own when the provider hands
 * out some other codec for the entity.
 */
public class TestRefreshCodec {
    @ParameterizedTest
    @EnumSource(MapperType.class)
    void reusesTheRegularPlan(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            CodecRegistry registry = datastore.getCodecRegistry();
            MorphiaCodecProvider provider = new MorphiaCodecProvider(datastore);

            MorphiaCodec<?> regular = (MorphiaCodec<?>) provider.get(HarnessReferences.class, registry);
            MorphiaCodec<?> refresh = (MorphiaCodec<?>) provider.getRefreshCodec(HarnessReferences.sample(), registry);

            Assertions.assertNotSame(regular, refresh);
            Assertions.assertSame(regular.getPlan(), refresh.getPlan());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void buildsItsOwnPlanForAnotherCodec(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            MorphiaCodecProvider provider = new MorphiaCodecProvider(datastore) {
                @Override
                @SuppressWarnings("unchecked")
                public <T> Codec<T> get(Class<T> type, CodecRegistry registry) {
                    return (Codec<T>) new OpaqueCodec();
                }
            };

            MorphiaCodec<?> refresh = (MorphiaCodec<?>) provider.getRefreshCodec(HarnessReferences.sample(),
                    datastore.getCodecRegistry());

            Assertions.assertNotNull(refresh);
            Assertions.assertSame(datastore.getMapper().getEntityModel(HarnessReferences.class), refresh.getPlan().model());
        }
    }

    /**
     * Refresh reads the stored document through a {@code DocumentReader}. Scalars stored as strings must come out as the
     * plain codec reads them.
     */
    @ParameterizedTest
    @EnumSource(MapperType.class)
    void decodesDriftedScalars(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            CodecRegistry registry = datastore.getCodecRegistry();
            Codec<HarnessScalars> codec = registry.get(HarnessScalars.class);
            String json = """
                    {"active": "true", "age": "42", "score": "1.5"}""";

            HarnessScalars plain;
            RawBsonDocument raw = RawBsonDocument.parse(json);
            try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO())) {
                plain = codec.decode(reader, DecoderContext.builder().build());
            }

            HarnessScalars refreshed = new MorphiaCodecProvider(datastore)
                    .getRefreshCodec(new HarnessScalars(), registry)
                    .decode(new DocumentReader(Document.parse(json), datastore.getMapper().getConversions()),
                            DecoderContext.builder().checkedDiscriminator(true).build());

            Assertions.assertEquals(encoded(codec, plain), encoded(codec, refreshed));
        }
    }

    private static RawBsonDocument encoded(Codec<HarnessScalars> codec, HarnessScalars entity) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, EncoderContext.builder().build());
        }
        return new RawBsonDocument(buffer.toByteArray());
    }

    private static MorphiaDatastore datastore(MongoClient client, MapperType mapperType) {
        return new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                .database("refresh_codec")
                .mapper(mapperType)
                .packages(List.of(HarnessReferences.class.getPackageName()))
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false));
    }

    /**
     * Stands in for a codec an importer's provider might supply instead of a {@code MorphiaCodec}.
     */
    private static class OpaqueCodec implements Codec<HarnessReferences> {
        @Override
        public HarnessReferences decode(BsonReader reader, DecoderContext decoderContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void encode(BsonWriter writer, HarnessReferences value, EncoderContext encoderContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<HarnessReferences> getEncoderClass() {
            return HarnessReferences.class;
        }
    }
}
