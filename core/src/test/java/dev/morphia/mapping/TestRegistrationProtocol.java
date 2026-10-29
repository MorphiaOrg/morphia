package dev.morphia.mapping;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.mapping.validation.ConstraintViolationException;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers the registration protocol shared by both mappers: a model is registered at most once per type, and a model
 * that fails validation or discriminator registration doesn't stay registered.
 */
public class TestRegistrationProtocol {
    private static final int THREADS = 8;
    private static final int ROUNDS = 50;

    /**
     * Asserts that a CRITTER run really got a critter model; {@link CritterMapper} silently falls back to a reflective
     * model when generation fails.
     */
    private static EntityModel assertModelType(MapperType type, EntityModel model) {
        if (type == MapperType.CRITTER) {
            Assertions.assertInstanceOf(CritterEntityModel.class, model,
                    "Expected a critter model but got " + model.getClass().getName());
        }
        return model;
    }

    private static Mapper mapper(MapperType type) {
        MorphiaConfig config = MorphiaConfig.load().mapper(type);
        return type == MapperType.CRITTER ? new CritterMapper(config) : new ReflectiveMapper(config);
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void invalidModelIsNotRegistered(MapperType type) {
        Mapper mapper = mapper(type);

        Assertions.assertThrows(ConstraintViolationException.class, () -> mapper.mapEntity(TwoVersions.class));
        Assertions.assertFalse(mapper.isMapped(TwoVersions.class), "A model that failed validation should not stay registered");
        Assertions.assertTrue(mapper.getMappedEntities().stream().noneMatch(m -> m.getType() == TwoVersions.class));

        Assertions.assertThrows(ConstraintViolationException.class, () -> mapper.mapEntity(TwoVersions.class),
                "Mapping an invalid type again should fail again, not return the half-registered model");
        Assertions.assertThrows(ConstraintViolationException.class, () -> mapper.getEntityModel(TwoVersions.class));
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void invalidModelIsUnlinkedFromItsInterfaces(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel tagged = mapper.mapEntity(Tagged.class);

        Assertions.assertThrows(ConstraintViolationException.class, () -> mapper.mapEntity(TwoVersions.class));
        Assertions.assertThrows(ConstraintViolationException.class, () -> mapper.mapEntity(TwoVersions.class));
        Assertions.assertTrue(tagged.getSubtypes().stream().noneMatch(m -> m.getType() == TwoVersions.class),
                "A model that failed validation should not stay linked as a subtype");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void duplicateDiscriminatorIsNotRegistered(MapperType type) {
        Mapper mapper = mapper(type);
        assertModelType(type, mapper.mapEntity(First.class));

        Assertions.assertThrows(MappingException.class, () -> mapper.mapEntity(Second.class));
        Assertions.assertFalse(mapper.isMapped(Second.class), "A model with a duplicate discriminator should not stay registered");
        Assertions.assertEquals(First.class, mapper.getClass("shared"),
                "A rejected duplicate shouldn't replace the original discriminator mapping");
        Assertions.assertEquals(List.of(First.class), mapper.getClassesMappedToCollection("shared").stream()
                .map(EntityModel::getType)
                .toList());
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void concurrentMappingRegistersOneModel(MapperType type) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                Mapper mapper = mapper(type);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<EntityModel>> results = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    results.add(executor.submit(() -> {
                        start.await();
                        return mapper.mapEntity(Concurrent.class);
                    }));
                }
                start.countDown();

                EntityModel first = assertModelType(type, results.get(0).get());
                for (Future<EntityModel> result : results) {
                    Assertions.assertSame(first, result.get(), "Every thread should see the one registered model");
                }
                Assertions.assertSame(first, mapper.getEntityModel(Concurrent.class));
                Assertions.assertEquals(Concurrent.class, mapper.getClass(first.discriminator()));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void loadsConventionsOncePerMapper(MapperType type, @TempDir Path services) throws Exception {
        // a loader that registers CountingConvention, so discovery can be counted
        Path file = services.resolve("META-INF/services/" + MorphiaConvention.class.getName());
        Files.createDirectories(file.getParent());
        Files.writeString(file, CountingConvention.class.getName());
        try (URLClassLoader loader = new URLClassLoader(new URL[] { services.toUri().toURL() }, getClass().getClassLoader())) {
            CountingConvention.reset();
            MorphiaConfig config = MorphiaConfig.load().mapper(type);
            Mapper mapper = type == MapperType.CRITTER ? new CritterMapper(config, loader) : new ReflectiveMapper(config, loader);

            List<MorphiaConvention> conventions = mapper.getConventions();
            Assertions.assertEquals(1, conventions.size());
            Assertions.assertSame(conventions, mapper.getConventions());
            Assertions.assertSame(conventions, mapper.getConventions());
            Assertions.assertEquals(1, CountingConvention.CREATED.get(), "discovered once per mapper, however often asked");

            if (type == MapperType.REFLECTION) {
                // critter models don't run conventions (#4416)
                mapper.mapEntity(Concurrent.class);
                mapper.mapEntity(First.class);
                Assertions.assertEquals(List.of(Concurrent.class, First.class), CountingConvention.APPLIED);
                Assertions.assertEquals(1, CountingConvention.CREATED.get(), "every model reuses the mapper's conventions");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void discoversConventionsOnceUnderConcurrentFirstUse(MapperType type, @TempDir Path services) throws Exception {
        Path file = services.resolve("META-INF/services/" + MorphiaConvention.class.getName());
        Files.createDirectories(file.getParent());
        Files.writeString(file, CountingConvention.class.getName());
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try (URLClassLoader loader = new URLClassLoader(new URL[] { services.toUri().toURL() }, getClass().getClassLoader())) {
            for (int round = 0; round < ROUNDS; round++) {
                CountingConvention.reset();
                MorphiaConfig config = MorphiaConfig.load().mapper(type);
                Mapper mapper = type == MapperType.CRITTER ? new CritterMapper(config, loader) : new ReflectiveMapper(config, loader);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<List<MorphiaConvention>>> results = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    results.add(executor.submit(() -> {
                        start.await();
                        return mapper.getConventions();
                    }));
                }
                start.countDown();
                List<MorphiaConvention> first = results.get(0).get();
                for (Future<List<MorphiaConvention>> result : results) {
                    Assertions.assertSame(first, result.get());
                }
                Assertions.assertEquals(1, CountingConvention.CREATED.get(), "discovered once even when first used concurrently");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Counts how often it's instantiated and which models it's applied to.
     */
    public static class CountingConvention implements MorphiaConvention {
        static final AtomicInteger CREATED = new AtomicInteger();
        static final List<Class<?>> APPLIED = new CopyOnWriteArrayList<>();

        public CountingConvention() {
            CREATED.incrementAndGet();
        }

        static void reset() {
            CREATED.set(0);
            APPLIED.clear();
        }

        @Override
        public void apply(Mapper mapper, EntityModel model) {
            APPLIED.add(model.getType());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    public void registerKeepsTheFirstModel(MapperType type) {
        Mapper mapper = mapper(type);
        EntityModel original = assertModelType(type, mapper.mapEntity(Concurrent.class));

        Assertions.assertSame(original, mapper.register(new EntityModel(mapper, Concurrent.class)));
        Assertions.assertSame(original, mapper.getEntityModel(Concurrent.class));
    }

    @Entity
    static class TwoVersions implements Tagged {
        @Id
        private ObjectId id;
        @Version
        private long version;
        @Version
        private long otherVersion;
    }

    @Entity
    interface Tagged {
    }

    @Entity(value = "shared", discriminator = "shared")
    static class First {
        @Id
        private ObjectId id;
    }

    @Entity(value = "shared", discriminator = "shared")
    static class Second {
        @Id
        private ObjectId id;
    }

    @Entity
    static class Concurrent {
        @Id
        private ObjectId id;
        private String name;
    }
}
