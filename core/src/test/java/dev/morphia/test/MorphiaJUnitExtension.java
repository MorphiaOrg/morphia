package dev.morphia.test;

import com.mongodb.client.MongoDatabase;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.internal.MorphiaInternals;
import dev.morphia.test.TestBase.ZDTCodecProvider;
import dev.morphia.test.config.ManualMorphiaTestConfig;
import org.bson.Document;
import org.junit.jupiter.api.extension.*;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Collectors;

import static dev.morphia.test.TestBase.TEST_DB_NAME;
import static java.lang.String.format;
import static java.util.Arrays.stream;

/**
 * JUnit 5 Extension for Morphia test lifecycle management
 * Handles MongoDB container setup, database initialization, and cleanup
 */
public class MorphiaJUnitExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MorphiaJUnitExtension.class);
    private static final String MONGO_HOLDER_KEY = "mongoHolder";
    private static final String MORPHIA_CONTAINER_KEY = "morphiaContainer";
    private static final String MORPHIA_CONFIG_KEY = "morphiaConfig";

    // ThreadLocal to track current ExtensionContext for each test thread
    private static final ThreadLocal<ExtensionContext> CURRENT_CONTEXT = new ThreadLocal<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        // Initialize MongoDB container
        MongoHolder mongoHolder = initMongoDbContainer(false);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(MONGO_HOLDER_KEY, mongoHolder);

        // Build config - check if test class provides custom config
        MorphiaConfig config = getConfigForTestClass(context);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(MORPHIA_CONFIG_KEY, config);

        // Initialize Morphia container
        MorphiaContainer morphiaContainer = new MorphiaContainer(mongoHolder.getMongoClient(), config);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(MORPHIA_CONTAINER_KEY, morphiaContainer);

        LOG.info("MongoDB container initialized successfully with {}",
                config.getClass().getSimpleName().equals("ManualMorphiaTestConfig") ? "default" : "custom");
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Clean up Morphia container
        MorphiaContainer morphiaContainer = context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(MORPHIA_CONTAINER_KEY, MorphiaContainer.class);
        if (morphiaContainer != null) {
            morphiaContainer = null;
        }

        // Clean up MongoDB container
        MongoHolder mongoHolder = context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(MONGO_HOLDER_KEY, MongoHolder.class);
        if (mongoHolder != null) {
            mongoHolder.close();
        }

        LOG.info("MongoDB container cleanup completed");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // Set current context for this thread
        CURRENT_CONTEXT.set(context);

        MorphiaContainer morphiaContainer = getMorphiaContainer(context);
        MongoHolder mongoHolder = getMongoHolder(context);

        if (morphiaContainer != null && mongoHolder != null) {
            // Reset database profiling and clean up
            MongoDatabase db = mongoHolder.getMongoClient()
                    .getDatabase(morphiaContainer.getMorphiaConfig().database());
            db.runCommand(new Document("profile", 0).append("slowms", 0));
            db.drop();
            morphiaContainer.reset();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            // Additional cleanup after each test if needed
            LOG.debug("Post-test cleanup for method: {}", context.getDisplayName());
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            CURRENT_CONTEXT.remove();
        }
    }

    // Helper methods for accessing stored objects
    public static MongoHolder getMongoHolder(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(MONGO_HOLDER_KEY, MongoHolder.class);
    }

    public static MorphiaContainer getMorphiaContainer(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(MORPHIA_CONTAINER_KEY, MorphiaContainer.class);
    }

    public static MorphiaConfig getMorphiaConfig(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
                .get(MORPHIA_CONFIG_KEY, MorphiaConfig.class);
    }

    // Convenience methods that use the current thread's context
    public static ExtensionContext getCurrentContext() {
        ExtensionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("No ExtensionContext available for current thread. " +
                    "Make sure the test is annotated with @ExtendWith(MorphiaJUnitExtension.class)");
        }
        return context;
    }

    public static MongoHolder getCurrentMongoHolder() {
        return getMongoHolder(getCurrentContext());
    }

    public static MorphiaContainer getCurrentMorphiaContainer() {
        return getMorphiaContainer(getCurrentContext());
    }

    public static MorphiaConfig getCurrentMorphiaConfig() {
        return getMorphiaConfig(getCurrentContext());
    }

    // MongoDB container initialization
    private static MongoHolder initMongoDbContainer(boolean sharded) {
        String mongodb = System.getProperty("mongodb", "8.0.0");
        String connectionString;
        MongoDBContainer mongoDBContainer = null;

        if ("local".equals(mongodb)) {
            LOG.info("'local' mongodb property specified. Using local server.");
            connectionString = "mongodb://localhost:27017/" + TEST_DB_NAME;
        } else {
            int version = Semver.parse(mongodb).getMajor();
            DockerImageName imageName = DockerImageName.parse("mongo:" + version)
                    .asCompatibleSubstituteFor("mongo");

            LOG.info("Running tests using " + imageName);
            mongoDBContainer = new MongoDBContainer(imageName);
            if (sharded) {
                mongoDBContainer.withSharding();
            }
            mongoDBContainer.start();
            connectionString = mongoDBContainer.getReplicaSetUrl(TEST_DB_NAME);
        }
        return new MongoHolder(mongoDBContainer, connectionString);
    }

    /**
     * Gets the appropriate MorphiaConfig for the test class.
     * Checks if the test class is annotated with @CustomMorphiaConfig and implements MorphiaConfigProvider.
     * If so, uses the custom config; otherwise uses the default config.
     */
    private static MorphiaConfig getConfigForTestClass(ExtensionContext context) {
        Class<?> testClass = context.getTestClass().orElse(null);

        if (testClass != null && testClass.isAnnotationPresent(CustomMorphiaConfig.class)) {
            if (MorphiaConfigProvider.class.isAssignableFrom(testClass)) {
                try {
                    Object testInstance = testClass.getDeclaredConstructor().newInstance();
                    return ((MorphiaConfigProvider) testInstance).provideMorphiaConfig();
                } catch (Exception e) {
                    LOG.warn("Failed to create custom MorphiaConfig for test class {}, using default config: {}",
                            testClass.getSimpleName(), e.getMessage());
                }
            } else {
                LOG.warn("Test class {} is annotated with @CustomMorphiaConfig but does not implement MorphiaConfigProvider, using default config",
                        testClass.getSimpleName());
            }
        }

        // Default configuration
        return buildConfig().codecProvider(new ZDTCodecProvider());
    }

    public static MorphiaConfig buildConfig(Class<?>... types) {
        MorphiaConfig config = new ManualMorphiaTestConfig()
                .database(TEST_DB_NAME);
        if (types.length != 0)
            config = config
                    .packages(stream(types)
                            .map(Class::getPackageName)
                            .collect(Collectors.toList()));
        return config;
    }

    // Version checking utilities
    public static void checkMinDriverVersion(String version) {
        checkMinDriverVersion(Semver.parse(version));
    }

    private static void checkMinDriverVersion(Semver semver) {
        Semver driverVersion = MorphiaInternals.getDriverVersion();
        if (driverVersion.isLowerThan(semver)) {
            throw new org.opentest4j.TestAbortedException(
                    format("Driver version %s is too old for this test. %s is required.", driverVersion, semver));
        }
    }

    public static void checkMinServerVersion(ExtensionContext context, String version) {
        checkMinServerVersion(context, Semver.parse(version));
    }

    private static void checkMinServerVersion(ExtensionContext context, Semver version) {
        MongoHolder mongoHolder = getMongoHolder(context);
        if (mongoHolder != null) {
            Semver serverVersion = getServerVersion();
            if (serverVersion.isLowerThan(version)) {
                throw new org.opentest4j.TestAbortedException(
                        format("Server version %s is too old for this test. %s is required.", serverVersion, version));
            }
        }
    }

    protected static Semver getServerVersion() {
        return getCurrentMorphiaContainer().getServerVersion();
    }

    public static void checkMaxServerVersion(ExtensionContext context, String version) {
        checkMaxServerVersion(context, Semver.parse(version));
    }

    private static void checkMaxServerVersion(ExtensionContext context, Semver version) {
        MongoHolder mongoHolder = getMongoHolder(context);
        if (mongoHolder != null) {
            Semver serverVersion = getServerVersion();
            if (serverVersion.isGreaterThan(version)) {
                throw new org.opentest4j.TestAbortedException(
                        format("Server version %s is too new for this test. %s is the maximum.", serverVersion, version));
            }
        }
    }
}