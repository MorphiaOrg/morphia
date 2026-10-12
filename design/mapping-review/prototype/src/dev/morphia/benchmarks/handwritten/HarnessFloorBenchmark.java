package dev.morphia.benchmarks.handwritten;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.benchmarks.Fixtures;
import dev.morphia.benchmarks.MapperVariant;
import dev.morphia.benchmarks.Models;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonType;
import org.bson.codecs.Codec;
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
import org.openjdk.jmh.annotations.Warmup;

/**
 * The part of {@code CodecBenchmark}'s cost no codec design can remove: allocating the buffer/writer/reader, and walking
 * the encoded document (reading every name, skipping every value). Same buffers and sizes as {@code CodecBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class HarnessFloorBenchmark {
    @Param({ "SIMPLE", "NESTED" })
    public Models model;

    private byte[] encoded;

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() {
        MorphiaConfig config = Fixtures.config(MapperVariant.REFLECTION);
        Mapper mapper = Fixtures.mappedMapper(MapperVariant.REFLECTION, config);
        try (MongoClient client = Fixtures.offlineClient()) {
            Codec<Object> codec = (Codec<Object>) Fixtures.codecRegistry(client, config, mapper).get(model.type());
            encoded = Verify.encode(codec, model.sample(), EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        }
    }

    /** Buffer + writer + an empty document + copy-out: the fixed per-call cost of the encode harness. */
    @Benchmark
    public byte[] encodeEmpty() {
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            writer.writeStartDocument();
            writer.writeEndDocument();
        }
        return buffer.toByteArray();
    }

    /** Reader + reading every top-level name and skipping every value (embedded documents are skipped whole). */
    @Benchmark
    public int decodeWalk() {
        int names = 0;
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(encoded))) {
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                names += reader.readName().length();
                reader.skipValue();
            }
            reader.readEndDocument();
        }
        return names;
    }
}
