package dev.morphia.mapping;

import java.io.File;
import java.lang.ref.Reference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.NestmateAccessException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

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
    public void testMappersShareRuntimeModels() {
        EntityModel first = mapper().mapEntity(CritterMapperTestEntity.class);
        EntityModel second = mapper().mapEntity(CritterMapperTestEntity.class);

        Assertions.assertInstanceOf(CritterClassLoader.class, first.getClass().getClassLoader(),
                "Expected a runtime-generated model");
        Assertions.assertSame(first.getClass(), second.getClass(),
                "Mappers with equivalent configs should reuse the generated model class");
        Assertions.assertNotSame(first, second, "Each mapper still gets its own model instance");
    }

    @Test
    public void testDifferentNamingDoesNotShareRuntimeModels() {
        EntityModel camelCase = mapper().mapEntity(CritterMapperTestEntity.class);
        EntityModel title = new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(NamingStrategy.title()))
                .mapEntity(CritterMapperTestEntity.class);

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

        EntityModel copied = copy.mapEntity(CritterMapperTestEntity.class);
        EntityModel camelCase = original.mapEntity(CritterMapperTestEntity.class);

        Assertions.assertInstanceOf(CritterClassLoader.class, copied.getClass().getClassLoader(),
                "Expected a runtime-generated model");
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

        EntityModel camelCase = original.mapEntity(CritterMapperTestEntity.class);
        EntityModel copied = copy.mapEntity(CritterMapperTestEntity.class);

        Assertions.assertInstanceOf(CritterClassLoader.class, copied.getClass().getClassLoader(),
                "Expected a runtime-generated model");
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

        EntityModel copied = copy.mapEntity(CritterMapperTestEntity.class);
        EntityModel model = original.mapEntity(CritterMapperTestEntity.class);

        Assertions.assertInstanceOf(CritterClassLoader.class, copied.getClass().getClassLoader(),
                "Expected a runtime-generated model");
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
        EntityModel first = new CritterMapper(config).mapEntity(CritterMapperTestEntity.class);
        EntityModel second = new CritterMapper(config).mapEntity(CritterMapperTestEntity.class);

        Assertions.assertNotSame(first.getClass(), second.getClass(),
                "A custom strategy can't be compared safely, so its models aren't shared");
        Assertions.assertEquals("NAME", second.getProperty("name").getMappedName());
    }

    @Test
    public void testSharedRuntimeModelsKeepTheirOwnAccessors() {
        MorphiaConfig fieldsConfig = MorphiaConfig.load().mapper(MapperType.CRITTER);
        CritterMapper first = new CritterMapper(fieldsConfig);
        first.mapEntity(AccessorChoiceEntity.class);
        new CritterMapper(fieldsConfig.propertyDiscovery(PropertyDiscovery.METHODS)).mapEntity(AccessorChoiceEntity.class);

        // Reuses the first mapper's model class, which must still be bound to field accessors.
        EntityModel model = new CritterMapper(fieldsConfig).mapEntity(AccessorChoiceEntity.class);
        AccessorChoiceEntity entity = new AccessorChoiceEntity();
        model.getProperty("name").getAccessor().set(entity, "value");

        Assertions.assertEquals("value", model.getProperty("name").getAccessor().get(entity),
                "A FIELDS model must read the field, not the getter");
        Reference.reachabilityFence(first);
    }

    @Test
    public void testExternalEntityStandInsDoNotShareRuntimeModels() {
        EntityModel first = mapper().mapEntity(FirstStandIn.class);
        EntityModel second = mapper().mapEntity(SecondStandIn.class);

        Assertions.assertEquals("first_targets", first.collectionName());
        Assertions.assertEquals("second_targets", second.collectionName());
    }

    @Test
    public void testConfiguredStrategiesDoNotShareRuntimeModels() {
        EntityModel first = new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(new PrefixNaming("first_")))
                .mapEntity(CritterMapperTestEntity.class);
        EntityModel second = new CritterMapper(MorphiaConfig.load()
                .mapper(MapperType.CRITTER)
                .propertyNaming(new PrefixNaming("second_")))
                .mapEntity(CritterMapperTestEntity.class);

        Assertions.assertEquals("first_name", first.getProperty("name").getMappedName());
        Assertions.assertEquals("second_name", second.getProperty("name").getMappedName());
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
            CritterMapper mapper = new CritterMapper(MorphiaConfig.load().mapper(MapperType.CRITTER), loader);
            EntityModel model = mapper.mapEntity(loader.loadClass("app.IsolatedEntity"));

            Assertions.assertNotNull(model);
            Assertions.assertEquals(EntityModel.class, model.getClass(), "Expected a reflective EntityModel");
            Assertions.assertNotNull(model.getProperty("name"));
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

    private static void compile(Path output, String file, String source) throws Exception {
        Path sourceFile = output.resolveSibling(output.getFileName() + "-src").resolve(file);
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
        List<String> classpath = new ArrayList<>();
        for (Class<?> type : List.of(Entity.class, ObjectId.class)) {
            classpath.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
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

    @Test
    public void testMethodBasedPropertyRoundTrip() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = mapper.mapEntity(MethodsChild.class);
        PropertyModel property = model.getProperty("name");
        Assertions.assertNotNull(property, "Property 'name' must be discovered");
        MethodsChild instance = new MethodsChild();
        property.getAccessor().set(instance, "hello");
        Assertions.assertEquals("hello", property.getAccessor().get(instance),
                "Accessor must round-trip the value written via set()");
    }

    @Test
    public void testPrivateSuperclassGetterExcluded() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = mapper.mapEntity(PrivateGetterChild.class);
        Assertions.assertNotNull(model);
        Assertions.assertNull(model.getProperty("secret"),
                "Private getter in superclass must not be exposed as a property");
    }

    @Test
    public void testStaticGetterExcluded() {
        CritterMapper mapper = mapper();
        EntityModel model = mapper.mapEntity(StaticGetterEntity.class);
        Assertions.assertNotNull(model);
        Assertions.assertNull(model.getProperty("kind"),
                "Static getter must not be exposed as a property");
    }

    @Test
    public void testSubclassGetterShadowsSuperclassGetter() {
        CritterMapper mapper = new CritterMapper(
                MorphiaConfig.load().mapper(MapperType.CRITTER).propertyDiscovery(PropertyDiscovery.METHODS));
        EntityModel model = mapper.mapEntity(OverridingChild.class);
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
        EntityModel model = mapper.mapEntity(GrandChild.class);
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

    /**
     * A configurable strategy in Morphia's own namespace, which must not be mistaken for a built-in.
     */
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
