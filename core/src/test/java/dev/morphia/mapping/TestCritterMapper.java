package dev.morphia.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCritterMapper {

    private CritterMapper mapper() {
        return new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER));
    }

    @Test
    public void testRuntimeGenerationProducesCritterEntityModel() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model instanceof CritterEntityModel,
                "Expected CritterEntityModel but got: " + model.getClass().getName());
    }

    @Test
    public void testCollectionNameFromAnnotation() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertNotNull(model);
        Assertions.assertEquals("critter_test", model.collectionName());
    }

    @Test
    public void testMappedEntityCached() {
        CritterMapper mapper = mapper();
        EntityModel first = mapper.mapEntity(CritterMapperTestEntity.class);
        EntityModel second = mapper.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertNotNull(first);
        Assertions.assertSame(first, second, "mapEntity should return the same cached model on repeated calls");
    }

    @Test
    public void testCopySharesCritterModels() {
        CritterMapper original = mapper();
        EntityModel model = original.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model instanceof CritterEntityModel, "Original model must be a CritterEntityModel");

        CritterMapper copy = (CritterMapper) original.copy();
        EntityModel copiedModel = copy.getEntityModel(CritterMapperTestEntity.class);

        Assertions.assertNotNull(copiedModel, "copy() must carry over already-mapped entities");
        Assertions.assertTrue(copiedModel instanceof CritterEntityModel, "Copied model must remain a CritterEntityModel");
        Assertions.assertNotSame(copiedModel, model, "copy() creates independent model instances for isolation");
    }

    @Test
    public void testCopyHasIndependentDiscriminatorLookup() {
        CritterMapper original = mapper();
        original.mapEntity(CritterMapperTestEntity.class);
        CritterMapper copy = (CritterMapper) original.copy();
        Assertions.assertNotNull(copy.getEntityModel(CritterMapperTestEntity.class));
        Assertions.assertNotSame(copy.getDiscriminatorLookup(), original.getDiscriminatorLookup());
    }

    @Test
    public void testNullTypeReturnNull() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(null);
        Assertions.assertNull(model);
    }

    @Test
    public void testNonEntityClassReturnNull() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(String.class);
        Assertions.assertNull(model);
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

        Assertions.assertNotNull(model, "Should fall back to reflection and return a non-null model");
        Assertions.assertFalse(model instanceof CritterEntityModel,
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
            Assertions.assertSame(first, result, "All threads should see the same registered model");
        }
    }

    /**
     * Phase 6.2: verify that the session-datastore copy pattern works.
     * SessionDatastore calls super(datastore) → MorphiaDatastore(MorphiaDatastore) → mapper.copy().
     * The copy must be a CritterMapper and must carry over already-mapped entities.
     */
    @Test
    public void testSessionDatastoreCopyPattern() {
        CritterMapper original = mapper();
        EntityModel model = original.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model instanceof CritterEntityModel);

        // Simulate what new MorphiaDatastore(datastore) does — calls mapper.copy()
        Mapper sessionMapper = original.copy();

        Assertions.assertTrue(sessionMapper instanceof CritterMapper,
                "copy() must return a CritterMapper for the session datastore");
        Assertions.assertTrue(sessionMapper.isMapped(CritterMapperTestEntity.class),
                "Session copy must preserve already-mapped entities");
        EntityModel sessionModel = sessionMapper.getEntityModel(CritterMapperTestEntity.class);
        Assertions.assertNotNull(sessionModel, "Session copy must preserve already-mapped entities");
        Assertions.assertTrue(sessionModel instanceof CritterEntityModel,
                "Session copy must produce CritterEntityModel instances, not reflection fallbacks");
        Assertions.assertNotSame(sessionModel, model, "Session copy creates independent model instances for isolation");
    }

    /**
     * Phase 6.3: verify that register() works as importModels() uses it.
     * importModels() calls mapper.register(model) for each model returned by EntityModelImporter.
     * This must work regardless of mapper type since register() is in AbstractMapper.
     */
    @Test
    public void testRegisterWorksForImportedModels() {
        CritterMapper mapper = mapper();

        // Simulate what importModels() does: create a model externally and register it
        EntityModel imported = new EntityModel(mapper, CritterMapperTestEntity.class);
        EntityModel registered = mapper.register(imported);

        Assertions.assertNotNull(registered);
        Assertions.assertTrue(mapper.isMapped(CritterMapperTestEntity.class),
                "register() must make the entity discoverable via isMapped()");
        Assertions.assertSame(mapper.getEntityModel(CritterMapperTestEntity.class), registered,
                "register() must make the model retrievable, as importModels() relies on it");
    }

    /**
     * Verify that inherited getter/setter pairs are discovered when METHODS mode is used.
     * Before the fix, PropertyFinder.discoverPropertyMethods() only inspected the immediate
     * class's methods and missed getters defined in parent classes.
     */
    @Test
    public void testInheritedGetterDiscoveryInMethodsMode() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = mapper.mapEntity(MethodsChild.class);
        Assertions.assertNotNull(model, "mapEntity should return a model for MethodsChild");
        Assertions.assertTrue(model instanceof CritterEntityModel,
                "Expected CritterEntityModel but got: " + model.getClass().getName());
        Assertions.assertNotNull(model.getProperty("name"),
                "Property 'name' inherited from MethodsBase should be discovered in METHODS mode");
    }

    /** Base class with a getter/setter pair that the subclass inherits. */
    public static class MethodsBase {
        private transient String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity("methods_child")
    public static class MethodsChild extends MethodsBase {
        @Id
        ObjectId id;
    }

    /*
     * Verifies that CritterMapper correctly reports lifecycle methods.
     * Previously, GizmoEntityModelGenerator hard-coded hasLifecycle() to always return false,
     * silently skipping @PrePersist/@PostLoad/@PreLoad/@PostPersist callbacks for CritterMapper.
     */
    @Test
    public void testHasLifecycleDetectedByCritterMapper() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(LifecycleEntity.class);

        Assertions.assertNotNull(model);
        Assertions.assertTrue(model instanceof CritterEntityModel,
                "Expected CritterEntityModel but got: " + model.getClass().getName());
        Assertions.assertTrue(model.hasLifecycle(PrePersist.class),
                "CritterMapper must detect @PrePersist lifecycle methods on entities");
    }

    @Entity("lifecycle_test")
    static class LifecycleEntity {
        @Id
        private ObjectId id;

        @PrePersist
        public void prePersist() {
        }
    }
}
