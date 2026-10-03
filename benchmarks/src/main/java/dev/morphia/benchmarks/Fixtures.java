package dev.morphia.benchmarks;

import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.codecs.configuration.CodecRegistry;

/**
 * Shared setup. Nothing here talks to a MongoDB server: the client is only needed to build Morphia's codec registry,
 * and it never sends a command.
 */
public final class Fixtures {
    private Fixtures() {
    }

    public static MorphiaConfig config(MapperVariant variant) {
        return ManualMorphiaConfig.configure()
                .database("benchmarks")
                .mapper(variant.mapperType())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false);
    }

    /**
     * Creates a mapper for the variant, maps every benchmark model, and verifies the models came from the expected
     * source.
     */
    public static Mapper mappedMapper(MapperVariant variant, MorphiaConfig config) {
        Mapper mapper = variant.newMapper(config);
        mapper.map(Models.ALL_TYPES);
        variant.verify(mapper);
        return mapper;
    }

    /**
     * A client that never connects in practice: a single unreachable seed with heartbeats pushed out so that the
     * monitor thread stays quiet during measurement.
     */
    public static MongoClient offlineClient() {
        return MongoClients.create(MongoClientSettings.builder()
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .applyToServerSettings(server -> server.heartbeatFrequency(1, TimeUnit.HOURS))
                .applyToSocketSettings(socket -> socket.connectTimeout(100, TimeUnit.MILLISECONDS))
                .build());
    }

    public static CodecRegistry codecRegistry(MongoClient client, MorphiaConfig config, Mapper mapper) {
        return new MorphiaDatastore(client, config, mapper).getCodecRegistry();
    }
}
