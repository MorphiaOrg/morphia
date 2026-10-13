package dev.morphia.benchmarks;

import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.MorphiaDatastore;
import dev.morphia.benchmarks.models.NestedEntity;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.MorphiaQuery;

import org.bson.Document;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.in;

/**
 * Query building with no server: translating a user path to stored field names, and rendering a filtered query to the
 * document sent to the server.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class QueryBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    private MongoClient client;
    private MorphiaDatastore datastore;
    private Mapper mapper;
    private EntityModel model;

    @Setup(Level.Trial)
    public void setup() {
        MapperVariant mapperVariant = MapperVariant.of(variant);
        MorphiaConfig config = Fixtures.config(mapperVariant);
        mapper = Fixtures.mappedMapper(mapperVariant, config);
        client = Fixtures.offlineClient();
        datastore = new MorphiaDatastore(client, config, mapper);
        model = mapper.getEntityModel(NestedEntity.class);
        if (!"profile.address.city".equals(translatedPath())) {
            throw new IllegalStateException("unexpected path " + translatedPath());
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    /**
     * A three-segment path through two embedded types.
     */
    @Benchmark
    public String translatedPath() {
        return new PathTarget(mapper, model, "profile.address.city").translatedPath();
    }

    /**
     * Three filters, including the discriminator added for the entity type.
     */
    @Benchmark
    public Document queryDocument() {
        return ((MorphiaQuery<NestedEntity>) datastore.find(NestedEntity.class)
                .filter(eq("name", "Nested"),
                        eq("home.city", "Springfield"),
                        in("profile.interests", java.util.List.of("jmh", "bson"))))
                .toDocument();
    }
}
