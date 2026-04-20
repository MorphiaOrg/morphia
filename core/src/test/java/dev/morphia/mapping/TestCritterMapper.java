package dev.morphia.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestCritterMapper {

    private CritterMapper mapper() {
        return new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER));
    }

    @Test
    public void testRuntimeGenerationProducesCritterEntityModel() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);
        assertNotNull(model);
        assertTrue(model instanceof CritterEntityModel,
                "Expected CritterEntityModel but got: " + model.getClass().getName());
    }

    @Test
    public void testCollectionNameFromAnnotation() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);
        assertNotNull(model);
        assertEquals(model.collectionName(), "critter_test");
    }

    @Test
    public void testMappedEntityCached() {
        CritterMapper mapper = mapper();
        EntityModel first = mapper.mapEntity(CritterMapperTestEntity.class);
        EntityModel second = mapper.mapEntity(CritterMapperTestEntity.class);
        assertNotNull(first);
        assertSame(first, second, "mapEntity should return the same cached model on repeated calls");
    }

    @Test
    public void testCopySharesCritterModels() {
        CritterMapper original = mapper();
        EntityModel model = original.mapEntity(CritterMapperTestEntity.class);
        assertNotNull(model);

        CritterMapper copy = (CritterMapper) original.copy();
        EntityModel copiedModel = copy.getEntityModel(CritterMapperTestEntity.class);

        assertSame(model, copiedModel, "copy() should share CritterEntityModel references");
    }

    @Test
    public void testCopyHasIndependentDiscriminatorLookup() {
        CritterMapper original = mapper();
        original.mapEntity(CritterMapperTestEntity.class);
        CritterMapper copy = (CritterMapper) original.copy();
        assertNotNull(copy.getEntityModel(CritterMapperTestEntity.class));
        assertNotSame(original.getDiscriminatorLookup(), copy.getDiscriminatorLookup());
    }

    @Test
    public void testNullTypeReturnNull() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(null);
        assertNull(model);
    }

    @Test
    public void testNonEntityClassReturnNull() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(String.class);
        assertNull(model);
    }

    @Test
    public void testReflectionFallbackWhenGenerationFails() {
        // A CritterClassLoader that refuses to load generated classes, forcing
        // tryRuntimeGeneration to fail and fall through to reflection.
        CritterClassLoader failingLoader = new CritterClassLoader(Thread.currentThread().getContextClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.contains("__morphia")) {
                    throw new ClassNotFoundException("Simulated generation failure: " + name);
                }
                return super.loadClass(name);
            }
        };

        CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER), failingLoader);
        EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);

        assertNotNull(model, "Should fall back to reflection and return a non-null model");
        assertFalse(model instanceof CritterEntityModel,
                "Fallback model should be a plain EntityModel, not CritterEntityModel");
    }

    @Test
    public void testConcurrentMappingProducesSingleModel() throws Exception {
        CritterMapper mapper = mapper();
        int threads = 8;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Future<EntityModel>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                latch.await();
                return mapper.mapEntity(CritterMapperTestEntity.class);
            }));
        }

        latch.countDown();
        List<EntityModel> results = new ArrayList<>();
        try {
            for (Future<EntityModel> f : futures) {
                results.add(f.get());
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        EntityModel first = results.get(0);
        for (EntityModel result : results) {
            assertSame(result, first, "All threads should see the same registered model");
        }
    }
}
