package dev.morphia.test;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static com.mongodb.MongoClientSettings.builder;

public class MongoExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger LOG = LoggerFactory.getLogger(MongoExtension.class);
    private static final Object LOCK = new Object();
    private static final int INVALID_OPTIONS = 72;
    private static MongoDBContainer container;
    private static MongoClient mongoClient;

    @Override
    public void beforeAll(ExtensionContext context) {
        synchronized (LOCK) {
            if (mongoClient != null) {
                return;
            }
            String mongodb = System.getProperty("mongodb", "8.0.0");
            String connectionString;
            if ("local".equals(mongodb)) {
                LOG.info("'local' mongodb property specified. Using local server.");
                connectionString = "mongodb://localhost:27017/";
            } else {
                int version = Semver.parse(mongodb).getMajor();
                DockerImageName imageName = DockerImageName.parse("mongo:" + version)
                        .asCompatibleSubstituteFor("mongo");
                LOG.info("Running tests using " + imageName);
                container = new MongoDBContainer(imageName);
                container.start();
                connectionString = container.getConnectionString();
            }
            mongoClient = MongoClients.create(builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyConnectionString(new ConnectionString(connectionString))
                    .build());
            LOG.info("Connected to MongoDB on port "
                    + mongoClient.getClusterDescription().getClusterSettings().getHosts().get(0).getPort());
            extendProfilerLockDeadline();
        }
    }

    /**
     * Starting with 8.3 (and later 8.2 patch releases), the server abandons a profiler write if it can't get the lock
     * on system.profile within internalQueryGlobalProfilingLockDeadlineMs (1 ms by default). On a loaded CI runner
     * that silently drops the entries that getLoggedQuery() and the profiling tests read back, so give it more time.
     * Older servers don't know the parameter and never abandon profiler writes.
     */
    private static void extendProfilerLockDeadline() {
        try {
            mongoClient.getDatabase("admin").runCommand(new Document("setParameter", 1)
                    .append("internalQueryGlobalProfilingLockDeadlineMs", 5000));
        } catch (MongoCommandException e) {
            if (e.getErrorCode() != INVALID_OPTIONS) {
                throw e;
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        mongoClient.getDatabase(
                context.getRequiredTestClass().getName().replaceFirst("^dev\\.morphia\\.", "").replace('.', '_')).drop();
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }
}
