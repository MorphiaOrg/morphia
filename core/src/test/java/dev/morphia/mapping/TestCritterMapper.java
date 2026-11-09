package dev.morphia.mapping;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.CritterGenerator;
import dev.morphia.critter.parser.generator.NestmateAccessException;
import dev.morphia.critter.parser.generator.NestmateAccessorRegistry;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.ReflectiveEntityModelBuilder;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class TestCritterMapper {

    private CritterMapper mapper() {
        return new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER));
    }

    /**
     * Asserts that a model came from runtime generation rather than an AOT model or the reflection fallback. The
     * entities here have no AOT models only because their package isn't in the test {@code morphia.packages}, so this
     * keeps the tests honest if that ever changes.
     */
    private static EntityModel assertRuntimeGenerated(EntityModel model) {
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(CritterEntityModel.class, model,
                "Expected a CritterEntityModel but got: " + model.getClass().getName());
        Assertions.assertInstanceOf(CritterClassLoader.class, model.getClass().getClassLoader(),
                "Expected a runtime-generated model but " + model.getClass().getName() + " was loaded by "
                        + model.getClass().getClassLoader());
        return model;
    }

    @Test
    public void testRuntimeGenerationProducesCritterEntityModel() {
        CritterMapper mapper = mapper();
        assertRuntimeGenerated(mapper.mapEntity(CritterMapperTestEntity.class));
    }

    @Test
    public void testCollectionNameFromAnnotation() {
        CritterMapper mapper = mapper();
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(CritterMapperTestEntity.class));
        Assertions.assertEquals("critter_test", model.collectionName());
    }

    @Test
    public void testMappedEntityCached() {
        CritterMapper mapper = mapper();
        EntityModel first = assertRuntimeGenerated(mapper.mapEntity(CritterMapperTestEntity.class));
        EntityModel second = mapper.mapEntity(CritterMapperTestEntity.class);
        Assertions.assertSame(first, second, "mapEntity should return the same cached model on repeated calls");
    }

    @Test
    public void testCopySharesCritterModels() {
        CritterMapper original = mapper();
        EntityModel model = assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));

        CritterMapper copy = (CritterMapper) original.copy();
        EntityModel copiedModel = copy.getEntityModel(CritterMapperTestEntity.class);

        Assertions.assertNotNull(copiedModel, "copy() must carry over already-mapped entities");
        assertRuntimeGenerated(copiedModel);
        Assertions.assertNotSame(copiedModel, model, "copy() creates independent model instances for isolation");
    }

    @Test
    public void testMappersShareRuntimeModels() {
        EntityModel first = assertRuntimeGenerated(mapper().mapEntity(CritterMapperTestEntity.class));
        EntityModel second = assertRuntimeGenerated(mapper().mapEntity(CritterMapperTestEntity.class));

        Assertions.assertSame(first.getClass(), second.getClass(),
                "Mappers with equivalent configs should reuse the generated model class");
        Assertions.assertNotSame(first, second, "Each mapper still gets its own model instance");
    }

    @Test
    public void testDifferentNamingDoesNotShareRuntimeModels() {
        EntityModel camelCase = assertRuntimeGenerated(mapper().mapEntity(CritterMapperTestEntity.class));
        EntityModel title = assertRuntimeGenerated(new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(NamingStrategy.title()))
                .mapEntity(CritterMapperTestEntity.class));

        Assertions.assertNotSame(camelCase.getClass(), title.getClass());
        Assertions.assertEquals("name", camelCase.getProperty("name").getMappedName());
        Assertions.assertEquals("Name", title.getProperty("name").getMappedName());
    }

    @Test
    public void testCopyWithDifferentNamingGeneratesItsOwnModels() {
        CritterMapper original = mapper();
        CritterMapper copy = new CritterMapper(original, MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(NamingStrategy.title()));

        EntityModel copied = assertRuntimeGenerated(copy.mapEntity(CritterMapperTestEntity.class));
        EntityModel camelCase = assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));

        Assertions.assertEquals("Name", copied.getProperty("name").getMappedName(),
                "An entity first mapped through the copy must use the copy's naming");
        Assertions.assertEquals("name", camelCase.getProperty("name").getMappedName());
        Assertions.assertNotSame(camelCase.getClass(), copied.getClass());
    }

    @Test
    public void testCopyWithDifferentNamingUnderCritterClassLoaderParent() {
        CritterClassLoader parent = new CritterClassLoader(getClass().getClassLoader());
        CritterMapper original = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER), parent);
        CritterMapper copy = new CritterMapper(original, MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(NamingStrategy.title()));

        EntityModel camelCase = assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));
        EntityModel copied = assertRuntimeGenerated(copy.mapEntity(CritterMapperTestEntity.class));

        Assertions.assertEquals("name", camelCase.getProperty("name").getMappedName());
        Assertions.assertEquals("Name", copied.getProperty("name").getMappedName(),
                "The copy must not reuse the model the original defined in the shared parent loader");
        Assertions.assertNotSame(camelCase.getClass(), copied.getClass());
    }

    @Test
    public void testCopyWithDifferentDatabaseSharesRuntimeModels() {
        CritterMapper original = mapper();
        CritterMapper copy = new CritterMapper(original, MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .database("copy_database"));

        EntityModel copied = assertRuntimeGenerated(copy.mapEntity(CritterMapperTestEntity.class));
        EntityModel model = assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));

        Assertions.assertSame(model.getClass(), copied.getClass(),
                "A copy that only changes operational settings should reuse the generated model class");
        Assertions.assertNotSame(model, copied);
    }

    @Test
    public void testCustomNamingDoesNotShareRuntimeModels() {
        NamingStrategy upperCase = new NamingStrategy() {
            @Override
            public String apply(String value) {
                return value.toUpperCase();
            }
        };
        MorphiaConfig config = MorphiaConfig.load().mapper(MapperType.CRITTER).propertyNaming(upperCase);
        EntityModel first = assertRuntimeGenerated(new CritterMapper(config).mapEntity(CritterMapperTestEntity.class));
        EntityModel second = assertRuntimeGenerated(new CritterMapper(config).mapEntity(CritterMapperTestEntity.class));

        Assertions.assertNotSame(first.getClass(), second.getClass(),
                "A custom strategy can't be compared safely, so its models aren't shared");
        Assertions.assertEquals("NAME", second.getProperty("name").getMappedName());
    }

    @Test
    public void testSharedRuntimeModelsKeepTheirOwnAccessors() {
        MorphiaConfig fieldsConfig = MorphiaConfig.load().mapper(MapperType.CRITTER);
        CritterMapper first = new CritterMapper(fieldsConfig);
        assertRuntimeGenerated(first.mapEntity(AccessorChoiceEntity.class));
        assertRuntimeGenerated(new CritterMapper(fieldsConfig.propertyDiscovery(PropertyDiscovery.METHODS))
                .mapEntity(AccessorChoiceEntity.class));

        // Reuses the first mapper's model class, which must still be bound to field accessors.
        EntityModel model = assertRuntimeGenerated(new CritterMapper(fieldsConfig).mapEntity(AccessorChoiceEntity.class));
        AccessorChoiceEntity entity = new AccessorChoiceEntity();
        model.getProperty("name").getAccessor().set(entity, "value");

        Assertions.assertEquals("value", model.getProperty("name").getAccessor().get(entity),
                "A FIELDS model must read the field, not the getter");
        Reference.reachabilityFence(first);
    }

    @Test
    public void testExternalEntityStandInsDoNotShareRuntimeModels() {
        EntityModel first = assertRuntimeGenerated(mapper().mapEntity(FirstStandIn.class));
        EntityModel second = assertRuntimeGenerated(mapper().mapEntity(SecondStandIn.class));

        Assertions.assertEquals("first_targets", first.collectionName());
        Assertions.assertEquals("second_targets", second.collectionName());
    }

    @Test
    public void testStandInPairWithPrivateTargetSetterUsesReflectiveAccessor() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        // the generated accessor would call the target's setter, which is private, so only that property is reflective
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(PrivateSetterStandIn.class));
        Assertions.assertInstanceOf(MethodAccessor.class, model.getProperty("name").getAccessor());
        Assertions.assertFalse(model.getProperty("id").getAccessor() instanceof MethodAccessor);
        PrivateSetterTarget instance = new PrivateSetterTarget();
        model.getProperty("name").getAccessor().set(instance, "written");
        Assertions.assertEquals("written", instance.getName());
    }

    @Test
    public void testStandInFromChildLoaderWithFluentSetter(@TempDir Path dir) throws Exception {
        // the stand-in's fluent setter returns the stand-in, which the target's loader can't see
        compile(dir, "app/FluentStandIn.java", """
                package app;
                @dev.morphia.annotations.ExternalEntity(target = dev.morphia.mapping.TestCritterMapper.PrivateSetterTarget.class,
                        value = "fluent_stand_ins")
                public class FluentStandIn {
                    private org.bson.types.ObjectId id;
                    private String name;
                    @dev.morphia.annotations.Id
                    public org.bson.types.ObjectId getId() {
                        return id;
                    }
                    public void setId(org.bson.types.ObjectId id) {
                        this.id = id;
                    }
                    public String getName() {
                        return name;
                    }
                    public FluentStandIn setName(String name) {
                        this.name = name;
                        return this;
                    }
                }
                """, Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
        try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, getClass().getClassLoader())) {
            CritterMapper mapper = new CritterMapper(
                    MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS), loader);
            EntityModel model = assertRuntimeGenerated(mapper.mapEntity(loader.loadClass("app.FluentStandIn")));
            PrivateSetterTarget instance = new PrivateSetterTarget();
            model.getProperty("name").getAccessor().set(instance, "written");
            Assertions.assertEquals("written", instance.getName());
        }
    }

    @Test
    public void testStandInSetterOnlyAsBridge() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        // the stand-in's setName(Object) doesn't take the getter's type, so its accessor is reflective, and the target has
        // setName(Object) only as the bridge for Named<String>.setName
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(BridgeSetterStandIn.class));
        BridgeSetterTarget instance = new BridgeSetterTarget();
        model.getProperty("name").getAccessor().set(instance, "bridged");
        Assertions.assertEquals("bridged", instance.getName());
    }

    @Test
    public void testUncallablePairUsesReflectiveAccessor() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        // the setter takes a supertype of the getter's type, which the generated accessors can't call
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(WideningSetterEntity.class));
        PropertyAccessor<Object> accessor = model.getProperty("names").getAccessor();
        Assertions.assertInstanceOf(MethodAccessor.class, accessor);
        WideningSetterEntity instance = new WideningSetterEntity();
        accessor.set(instance, List.of("a", "b"));
        Assertions.assertEquals(List.of("a", "b"), accessor.get(instance));
    }

    @Test
    public void testFailedModelGenerationLeavesNoAccessors() {
        // the entity model is generated after the accessors' checks pass, and naming its collection fails
        CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER)
                .collectionNaming(new FailingNaming()));
        CritterClassLoader runtime = new CritterClassLoader(getClass().getClassLoader());
        Assertions.assertThrows(IllegalStateException.class,
                () -> new CritterGenerator(mapper).generate(HiddenAnnotationEntity.class, runtime, true));
        Assertions.assertNull(NestmateAccessorRegistry.get(runtime,
                Critter.critterPackage(HiddenAnnotationEntity.class) + ".IdAccessor"));

        CritterClassLoader buildTime = new CritterClassLoader(getClass().getClassLoader());
        Assertions.assertThrows(IllegalStateException.class,
                () -> new CritterGenerator(mapper).generate(HiddenAnnotationEntity.class, buildTime, false));
        Assertions.assertEquals(Map.of(), buildTime.getTypeDefinitions());
    }

    @Test
    public void testNonPublicAnnotationIsCopied() {
        CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER));
        EntityModel model = mapper.mapEntity(HiddenAnnotationEntity.class);
        Assertions.assertInstanceOf(CritterEntityModel.class, model);
        Assertions.assertEquals("hidden", model.getProperty("name").getAnnotation(HiddenAnnotation.class).value());
    }

    @Test
    public void testMissingSuperclassClassFileFailsGeneration(@TempDir Path dir) throws Exception {
        compile(dir, "app/HiddenParent.java", """
                package app;
                public class HiddenParent {
                    private String inherited;
                }
                """);
        compile(dir, "app/VisibleChild.java", """
                package app;
                @dev.morphia.annotations.Entity
                public class VisibleChild extends HiddenParent {
                    @dev.morphia.annotations.Id
                    private org.bson.types.ObjectId id;
                }
                """, dir);
        try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, getClass().getClassLoader()) {
            @Override
            public URL findResource(String name) {
                return name.equals("app/HiddenParent.class") ? null : super.findResource(name);
            }
        }) {
            Class<?> child = loader.loadClass("app.VisibleChild");
            CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER), loader);
            // without the superclass's classfile, its properties would be silently left out
            UnsupportedOperationException e = Assertions.assertThrows(UnsupportedOperationException.class,
                    () -> new CritterGenerator(mapper).generate(child, new CritterClassLoader(loader), false));
            Assertions.assertTrue(e.getMessage().contains("app.HiddenParent"), e.getMessage());
        }
    }

    @Test
    public void testGenerationReadsEachClassFileOnce(@TempDir Path dir) throws Exception {
        compile(dir, "app/SharedParent.java", """
                package app;
                public class SharedParent {
                    private String inherited;
                }
                """);
        compile(dir, "app/ReadOnceChild.java", """
                package app;
                @dev.morphia.annotations.Entity
                public class ReadOnceChild extends SharedParent {
                    @dev.morphia.annotations.Id
                    private org.bson.types.ObjectId id;
                }
                """, dir);
        Map<String, Integer> reads = new ConcurrentHashMap<>();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                reads.merge(name, 1, Integer::sum);
                return super.getResourceAsStream(name);
            }
        }) {
            Class<?> child = loader.loadClass("app.ReadOnceChild");
            CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER), loader);
            // build-time generation walks the hierarchy to discover fields, look for an @Id getter and weave accessors
            new CritterGenerator(mapper).generate(child, new CritterClassLoader(loader), false);
        }
        Assertions.assertEquals(Map.of("app/ReadOnceChild.class", 1, "app/SharedParent.class", 1), reads);
    }

    @Test
    public void testConfiguredStrategiesDoNotShareRuntimeModels() {
        EntityModel first = assertRuntimeGenerated(new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(new PrefixNaming("first_")))
                .mapEntity(CritterMapperTestEntity.class));
        EntityModel second = assertRuntimeGenerated(new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(new PrefixNaming("second_")))
                .mapEntity(CritterMapperTestEntity.class));

        Assertions.assertEquals("first_name", first.getProperty("name").getMappedName());
        Assertions.assertEquals("second_name", second.getProperty("name").getMappedName());
    }

    @Test
    public void testCopyHasIndependentDiscriminatorLookup() {
        CritterMapper original = mapper();
        assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));
        CritterMapper copy = (CritterMapper) original.copy();
        assertRuntimeGenerated(copy.getEntityModel(CritterMapperTestEntity.class));
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
    public void testEntityOutsideMorphiasClassLoaderFallsBackToReflection(@TempDir Path dir) throws Exception {
        // An entity loaded by its own class loader is in a different unnamed module than Morphia, so runtime generation
        // can't define nestmate accessors for it.
        compile(dir, "app/IsolatedEntity.java", """
                package app;
                @dev.morphia.annotations.Entity
                public class IsolatedEntity {
                    @dev.morphia.annotations.Id
                    private org.bson.types.ObjectId id;
                    private String name;
                }
                """);

        // logback-test.xml sets dev.morphia to ERROR, so the WARN would be dropped before reaching any appender. Test
        // classes run concurrently, so other threads' CritterMapper warnings can reach this appender too while it's attached;
        // only this thread's events are counted (mapping and its warning happen synchronously on the calling thread).
        Logger logger = (Logger) LoggerFactory.getLogger(CritterMapper.class);
        Level level = logger.getLevel();
        String thread = Thread.currentThread().getName();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try (URLClassLoader loader = new URLClassLoader(new URL[] { dir.toUri().toURL() }, getClass().getClassLoader())) {
            MorphiaConfig config = MorphiaConfig.load().mapper(MapperType.CRITTER);
            CritterMapper mapper = new CritterMapper(config, loader);
            EntityModel model = mapper.mapEntity(loader.loadClass("app.IsolatedEntity"));

            Assertions.assertNotNull(model);
            Assertions.assertEquals(EntityModel.class, model.getClass(), "Expected a reflective EntityModel");
            Assertions.assertNotNull(model.getProperty("name"));

            // mappers with the same configuration remember the failure rather than retrying and logging it again
            EntityModel again = new CritterMapper(config, loader).mapEntity(loader.loadClass("app.IsolatedEntity"));
            Assertions.assertEquals(EntityModel.class, again.getClass(), "Expected a reflective EntityModel");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        List<String> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN && thread.equals(event.getThreadName()))
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("app.IsolatedEntity"))
                .toList();
        Assertions.assertEquals(1, warnings.size(), warnings.toString());
        String warning = warnings.get(0);
        Assertions.assertTrue(warning.contains("can't access app.IsolatedEntity"), warning);
        Assertions.assertTrue(warning.contains("isn't in Morphia's module"), warning);
        Assertions.assertTrue(warning.contains("critter-maven"), warning);
    }

    @Test
    public void testAccessFailureClassification() {
        NestmateAccessException accessException = new NestmateAccessException(String.class,
                new IllegalAccessException("does not have full privilege access"));

        Assertions.assertSame(accessException, CritterMapper.accessFailure(accessException));
        Assertions.assertSame(accessException,
                CritterMapper.accessFailure(new RuntimeException(new IllegalStateException(accessException))));
        Assertions.assertNull(CritterMapper.accessFailure(new RuntimeException(new IllegalAccessException("unrelated"))));
        Assertions.assertNull(CritterMapper.accessFailure(new ClassNotFoundException("unrelated")));
    }

    private static void compile(Path output, String file, String source, Path... classpathEntries) throws Exception {
        Path sourceFile = output.resolveSibling(output.getFileName() + "-src").resolve(file);
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
        List<String> classpath = new ArrayList<>();
        for (Class<?> type : List.of(Entity.class, ObjectId.class)) {
            classpath.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        for (Path entry : classpathEntries) {
            classpath.add(entry.toString());
        }
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", output.toString(), "-cp",
                String.join(File.pathSeparator, classpath), sourceFile.toString());
        Assertions.assertEquals(0, result, "Fixture compilation failed");
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
        EntityModel first = assertRuntimeGenerated(results.get(0));
        for (EntityModel result : results) {
            Assertions.assertSame(first, result, "All threads should see the same registered model");
        }
    }

    @Test
    public void testConcurrentMappingAcrossSharingMappers() throws Exception {
        // A discriminator key no other test uses gives these mappers a RuntimeModels of their own, so the model class
        // is generated during the race rather than reused from an earlier test.
        MorphiaConfig config = MorphiaConfig.load().mapper(MapperType.CRITTER).discriminatorKey("_concurrentShare");
        int threads = 8;
        List<CritterMapper> mappers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            mappers.add(new CritterMapper(config));
        }
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Future<EntityModel>> futures = new ArrayList<>();
        for (CritterMapper mapper : mappers) {
            futures.add(pool.submit(() -> {
                latch.await();
                return mapper.mapEntity(CritterMapperTestEntity.class);
            }));
        }

        latch.countDown();
        List<EntityModel> results = new ArrayList<>();
        try {
            for (Future<EntityModel> f : futures) {
                results.add(f.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        Class<?> modelClass = assertRuntimeGenerated(results.get(0)).getClass();
        for (int i = 0; i < threads; i++) {
            EntityModel result = assertRuntimeGenerated(results.get(i));
            Assertions.assertSame(modelClass, result.getClass(), "Sharing mappers should reuse one generated model class");
            Assertions.assertSame(result, mappers.get(i).getEntityModel(CritterMapperTestEntity.class),
                    "Each mapper should register the model it returned");
            for (int j = i + 1; j < threads; j++) {
                Assertions.assertNotSame(result, results.get(j), "Each mapper gets its own model instance");
            }
            Assertions.assertEquals("name", result.getProperty("name").getMappedName());
        }
    }

    @Test
    public void testRuntimeClassLoaderReleasedWithItsMappers() throws InterruptedException {
        WeakReference<ClassLoader> loader = mapAndForget();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (loader.get() != null && System.nanoTime() < deadline) {
            System.gc();
            Thread.sleep(50);
        }

        Assertions.assertNull(loader.get(),
                "The CritterClassLoader should be collectable once no mapper uses it; something still holds it");
    }

    /**
     * Maps an entity through mappers that share runtime models and returns only a weak reference to the generated
     * loader, so nothing on the caller's stack keeps the mappers, models, or loader alive. The discriminator key is one
     * no other test uses, so no other mapper in this JVM shares the loader.
     */
    private static WeakReference<ClassLoader> mapAndForget() {
        MorphiaConfig config = MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .collectionNaming(NamingStrategy.kebabCase())
                .discriminatorKey("_releaseCheck");
        CritterMapper mapper = new CritterMapper(config);
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(ReleasedEntity.class));
        EntityModel shared = assertRuntimeGenerated(new CritterMapper(config).mapEntity(ReleasedEntity.class));
        Assertions.assertSame(model.getClass(), shared.getClass(), "Expected the two mappers to share runtime models");
        Assertions.assertSame(model.getClass(), mapper.copy().getEntityModel(ReleasedEntity.class).getClass());

        ReleasedEntity entity = new ReleasedEntity();
        model.getProperty("name").getAccessor().set(entity, "value");
        Assertions.assertEquals("value", model.getProperty("name").getAccessor().get(entity));

        return new WeakReference<>(model.getClass().getClassLoader());
    }

    /**
     * {@code copy()} returns a CritterMapper that carries over already-mapped entities as models of its own. Sessions
     * no longer copy the mapper (they share their datastore's), but the copy constructors still have callers.
     */
    @Test
    public void testCopyPreservesMappedEntities() {
        CritterMapper original = mapper();
        EntityModel model = assertRuntimeGenerated(original.mapEntity(CritterMapperTestEntity.class));

        Mapper copy = original.copy();

        Assertions.assertTrue(copy instanceof CritterMapper, "copy() must return a CritterMapper");
        Assertions.assertTrue(copy.isMapped(CritterMapperTestEntity.class), "The copy must preserve already-mapped entities");
        EntityModel copiedModel = copy.getEntityModel(CritterMapperTestEntity.class);
        Assertions.assertNotNull(copiedModel, "The copy must preserve already-mapped entities");
        assertRuntimeGenerated(copiedModel);
        Assertions.assertNotSame(copiedModel, model, "The copy has model instances of its own");
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
        EntityModel imported = new ReflectiveEntityModelBuilder(mapper, CritterMapperTestEntity.class).build();
        EntityModel registered = mapper.register(imported);

        Assertions.assertNotNull(registered);
        Assertions.assertTrue(mapper.isMapped(CritterMapperTestEntity.class),
                "register() must make the entity discoverable via isMapped()");
        Assertions.assertSame(mapper.getEntityModel(CritterMapperTestEntity.class), registered,
                "register() must make the model retrievable, as importModels() relies on it");
    }

    @Test
    public void testInheritedGetterDiscoveryInMethodsMode() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(MethodsChild.class));
        Assertions.assertNotNull(model.getProperty("name"),
                "Property 'name' inherited from MethodsBase should be discovered in METHODS mode");
    }

    @Test
    public void testMethodBasedPropertyRoundTrip() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(MethodsChild.class));
        PropertyModel property = model.getProperty("name");
        Assertions.assertNotNull(property, "Property 'name' must be discovered");
        MethodsChild instance = new MethodsChild();
        property.getAccessor().set(instance, "hello");
        Assertions.assertEquals("hello", property.getAccessor().get(instance),
                "Accessor must round-trip the value written via set()");
    }

    @Test
    public void testPrivateSuperclassGetter() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        // the accessor is a nestmate of the superclass, so it can call the private getter
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(PrivateGetterChild.class));
        PropertyAccessor<Object> accessor = model.getProperty("secret").getAccessor();
        PrivateGetterChild instance = new PrivateGetterChild();
        accessor.set(instance, "hidden");
        Assertions.assertEquals("hidden", accessor.get(instance));
    }

    @Test
    public void testPublicPairFromAnotherModule() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        // java.base doesn't open java.util, so the accessor for Date's public getTime()/setTime() must join the entity's nest
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(DateEntity.class));
        PropertyAccessor<Object> accessor = model.getProperty("time").getAccessor();
        DateEntity instance = new DateEntity();
        accessor.set(instance, 42L);
        Assertions.assertEquals(42L, accessor.get(instance));
    }

    @Test
    public void testFluentSetter() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(FluentSetterEntity.class));
        PropertyAccessor<Object> accessor = model.getProperty("name").getAccessor();
        FluentSetterEntity instance = new FluentSetterEntity();
        accessor.set(instance, "fluent");
        Assertions.assertEquals("fluent", accessor.get(instance));
        PropertyAccessor<Object> count = model.getProperty("count").getAccessor();
        count.set(instance, 42L);
        Assertions.assertEquals(42L, count.get(instance));
    }

    @Test
    public void testArrayProperty() {
        EntityModel model = assertRuntimeGenerated(mapper().mapEntity(ArrayEntity.class));
        PropertyAccessor<Object> accessor = model.getProperty("names").getAccessor();
        ArrayEntity instance = new ArrayEntity();
        // the codec decodes an array as a list, which the accessor converts to the property's type
        accessor.set(instance, List.of("a", "b"));
        Assertions.assertArrayEquals(new String[] { "a", "b" }, instance.names);
    }

    @Test
    public void testStaticGetterExcluded() {
        CritterMapper mapper = mapper();
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(StaticGetterEntity.class));
        Assertions.assertNull(model.getProperty("kind"),
                "Static getter must not be exposed as a property");
    }

    @Test
    public void testSubclassGetterShadowsSuperclassGetter() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(OverridingChild.class));
        Assertions.assertNotNull(model.getProperty("value"),
                "Property must be discovered when both subclass and superclass define the getter");
        OverridingChild instance = new OverridingChild();
        model.getProperty("value").getAccessor().set(instance, "x");
        Assertions.assertEquals("OVERRIDDEN:x", model.getProperty("value").getAccessor().get(instance),
                "Subclass getter must take precedence over superclass getter");
    }

    @Test
    public void testGrandparentSetterDiscovered() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(GrandChild.class));
        Assertions.assertNotNull(model.getProperty("data"),
                "Setter defined only in grandparent must be found via hierarchy walk");
        GrandChild instance = new GrandChild();
        model.getProperty("data").getAccessor().set(instance, "test");
        Assertions.assertEquals("test", model.getProperty("data").getAccessor().get(instance));
    }

    public static class MethodsBase {
        // transient: excluded from field-based discovery so only METHODS mode maps this property
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

    public static class PrivateGetterBase {
        private transient String secret;

        private String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    @Entity("private_getter_child")
    public static class PrivateGetterChild extends PrivateGetterBase {
        @Id
        ObjectId id;
    }

    @Entity("fluent_setter_entity")
    public static class FluentSetterEntity {
        @Id
        ObjectId id;
        private String name;
        private long count;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public FluentSetterEntity setName(String name) {
            this.name = name;
            return this;
        }

        public long getCount() {
            return count;
        }

        // returns the previous value, which takes two stack slots
        public long setCount(long count) {
            long previous = this.count;
            this.count = count;
            return previous;
        }
    }

    @Entity("date_entity")
    public static class DateEntity extends java.util.Date {
    }

    @Entity("array_entity")
    public static class ArrayEntity {
        @Id
        ObjectId id;
        String[] names;
    }

    @Entity("static_getter_entity")
    public static class StaticGetterEntity {
        @Id
        ObjectId id;

        public static String getKind() {
            return "example";
        }
    }

    /**
     * Verifies that CritterMapper correctly reports lifecycle methods.
     * Previously, EntityModelGenerator hard-coded hasLifecycle() to always return false,
     * silently skipping @PrePersist/@PostLoad/@PreLoad/@PostPersist callbacks for CritterMapper.
     */
    @Test
    public void testHasLifecycleDetectedByCritterMapper() {
        CritterMapper mapper = mapper();
        EntityModel model = assertRuntimeGenerated(mapper.mapEntity(LifecycleEntity.class));

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

    public static class OverridingBase {
        private transient String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Entity("overriding_child")
    public static class OverridingChild extends OverridingBase {
        @Id
        ObjectId id;

        @Override
        public String getValue() {
            String raw = super.getValue();
            return raw == null ? null : "OVERRIDDEN:" + raw;
        }
    }

    public static class GrandParent {
        private transient String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class MiddleParent extends GrandParent {
    }

    @Entity("grand_child")
    public static class GrandChild extends MiddleParent {
        @Id
        ObjectId id;
    }

    @Entity
    public static class AccessorChoiceEntity {
        @Id
        ObjectId id;
        private String name;

        public String getName() {
            return "getter:" + name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity("released_entity")
    public static class ReleasedEntity {
        @Id
        private ObjectId id;
        private String name;
    }

    public static class ExternalTarget {
        private ObjectId id;
        private String name;
    }

    @ExternalEntity(target = ExternalTarget.class, value = "first_targets")
    public static class FirstStandIn {
        @Id
        private ObjectId id;
        private String name;
    }

    @ExternalEntity(target = ExternalTarget.class, value = "second_targets")
    public static class SecondStandIn {
        @Id
        private ObjectId id;
        private String name;
    }

    public interface Named<T> {
        void setName(T name);
    }

    public static class PrivateSetterBase {
        @SuppressWarnings("unused")
        private Object other;

        @SuppressWarnings("unused")
        private void setName(Object name) {
            this.other = name;
        }
    }

    // the bridge for Named<String>.setName, not the superclass's private setName(Object), sets the name
    public static class BridgeSetterTarget extends PrivateSetterBase implements Named<String> {
        private ObjectId id;
        private String name;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }
    }

    @ExternalEntity(target = BridgeSetterTarget.class, value = "bridge_setter_targets")
    public static class BridgeSetterStandIn {
        private ObjectId id;
        private String name;

        @Id
        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(Object name) {
            this.name = (String) name;
        }
    }

    @Entity("widening_setters")
    public static class WideningSetterEntity {
        private ObjectId id;
        private List<String> names;

        @Id
        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public List<String> getNames() {
            return names;
        }

        public void setNames(Collection<String> names) {
            this.names = new ArrayList<>(names);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface HiddenAnnotation {
        String value();
    }

    @Entity
    public static class HiddenAnnotationEntity {
        @Id
        private ObjectId id;
        @HiddenAnnotation("hidden")
        private String name;
    }

    public static class PrivateSetterTarget {
        private ObjectId id;
        private String name;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }
    }

    @ExternalEntity(target = PrivateSetterTarget.class, value = "private_setter_targets")
    public static class PrivateSetterStandIn {
        private ObjectId id;
        private String name;

        @Id
        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * A configurable strategy in Morphia's own namespace, which must not be mistaken for a built-in.
     */
    public static class FailingNaming extends NamingStrategy {
        @Override
        public String apply(String value) {
            throw new IllegalStateException("Naming failed for " + value);
        }
    }

    public static class PrefixNaming extends NamingStrategy {
        private final String prefix;

        public PrefixNaming(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String apply(String value) {
            return prefix + value;
        }
    }
}
