package dev.morphia.benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.MorphiaDatastore;
import dev.morphia.benchmarks.models.SimpleEntity;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
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

/**
 * The per-session cost: {@code startSession()} and {@code withTransaction()} build a {@code SessionDatastore} through
 * the {@link MorphiaDatastore} copy constructor, which copies the mapper and rebuilds the codec registry. Starting a real
 * {@code ClientSession} needs a server, so this measures the copy constructor directly.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class DatastoreBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    private MongoClient client;
    private MorphiaDatastore datastore;
    private byte[] encoded;
    private final DecoderContext decoderContext = DecoderContext.builder().build();

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() {
        MapperVariant mapperVariant = MapperVariant.of(variant);
        MorphiaConfig config = Fixtures.config(mapperVariant);
        Mapper mapper = Fixtures.mappedMapper(mapperVariant, config);
        client = Fixtures.offlineClient();
        datastore = new MorphiaDatastore(client, config, mapper);
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            ((Codec<SimpleEntity>) datastore.getCodecRegistry().get(SimpleEntity.class))
                    .encode(writer, SimpleEntity.sample(), EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        }
        encoded = buffer.toByteArray();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    /**
     * What every session pays before it does anything.
     */
    @Benchmark
    public MorphiaDatastore copy() {
        return new MorphiaDatastore(datastore);
    }

    /**
     * A session that decodes one entity, which also rebuilds that entity's codec in the session's cold registry.
     */
    @Benchmark
    @SuppressWarnings("unchecked")
    public SimpleEntity copyAndDecode() {
        MorphiaDatastore session = new MorphiaDatastore(datastore);
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(encoded))) {
            return ((Codec<SimpleEntity>) session.getCodecRegistry().get(SimpleEntity.class)).decode(reader, decoderContext);
        }
    }
}
