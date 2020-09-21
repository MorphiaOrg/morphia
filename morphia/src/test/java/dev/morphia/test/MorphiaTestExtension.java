package dev.morphia.test;

import com.antwerkz.bottlerocket.BottleRocket;
import com.antwerkz.bottlerocket.clusters.ReplicaSet;
import com.antwerkz.bottlerocket.configuration.types.Verbosity;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;

public class MorphiaTestExtension implements TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback {
    protected static final String TEST_DB_NAME = "morphia_test";
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaTestExtension.class);

    private final MapperOptions mapperOptions = MapperOptions.DEFAULT;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private Datastore ds;

    @Override
    public void afterEach(ExtensionContext context) {
        cleanup();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        cleanup();
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getMongoClient().getDatabase(TEST_DB_NAME);
        }
        return database;
    }

    public Datastore getDatastore() {
        if (ds == null) {
            ds = Morphia.createDatastore(getMongoClient(), getDatabase().getName());
            System.out.println("****************** now ds = " + ds);
            //            ds.setQueryFactory(new DefaultQueryFactory());
        }
        return ds;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        inject(testInstance, "mongoClient", getMongoClient());
        inject(testInstance, "database", getDatabase());
        inject(testInstance, "ds", getDatastore());
    }

    private void cleanup() {
        MongoDatabase db = getDatabase();
        db.listCollectionNames().forEach(s -> {

            if (!s.equals("zipcodes")) {
                LOG.info("dropping collection " + s);
                db.getCollection(s).drop();
            }
        });
    }

    private Field findField(Class<?> testType, String fieldName) throws NoSuchFieldException {
        try {
            return testType.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (testType.equals(Object.class)) {
                throw e;
            }

            return findField(testType.getSuperclass(), fieldName);
        }
    }

    private MongoClient getMongoClient() {
        if (mongoClient == null) {
            startMongo();
        }
        return mongoClient;
    }

    private void inject(Object testInstance, String fieldName, Object value) {
        System.out.println("MorphiaTestExtension.inject");
        System.out.printf("testInstance = %s, fieldName = %s, value = %s%n", testInstance, fieldName, value);
        try {
            Field field = findField(testInstance.getClass(), fieldName);
            field.set(testInstance, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(String.format("Could not set field %s: %s", fieldName, e.getMessage()));
        }
    }

    private void startMongo() {
        Builder builder = MongoClientSettings.builder();

        try {
            builder.uuidRepresentation(mapperOptions.getUuidRepresentation());
        } catch (Exception ignored) {
            // not a 4.0 driver
        }

        String mongodb = System.getenv("MONGODB");
        Version version = mongodb != null ? Version.valueOf(mongodb) : BottleRocket.DEFAULT_VERSION;
        final ReplicaSet cluster = new ReplicaSet(new File("target/mongo/"), "morphia_test", version);
        //        cluster.addNode(new Configuration());
        //        cluster.addNode(new Configuration());

        cluster.configure(c -> {
            c.systemLog(s -> {
                s.setTraceAllExceptions(true);
                s.setVerbosity(Verbosity.FIVE);
                return null;
            });
            return null;
        });
        cluster.clean();
        cluster.start();
        mongoClient = cluster.getClient(builder);
    }
}
