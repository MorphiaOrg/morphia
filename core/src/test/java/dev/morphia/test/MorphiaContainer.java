package dev.morphia.test;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import dev.morphia.DatastoreImpl;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.test.TestBase.ZDTCodecProvider;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static com.mongodb.MongoClientSettings.builder;
import static dev.morphia.test.TestBase.TEST_DB_NAME;
import static java.lang.String.format;

@SuppressWarnings("removal")
public class MorphiaContainer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaContainer.class);
    private final boolean sharded;
    public String connectionString;
    public MongoDBContainer mongoDBContainer;
    DatastoreImpl datastore;
    MongoDatabase database;
    MapperOptions mapperOptions = MapperOptions.builder()
            .codecProvider(new ZDTCodecProvider())
            .build();;
    MongoClient mongoClient;

    public MorphiaContainer(boolean sharded) {
        this.sharded = sharded;
    }

    public MorphiaContainer mapperOptions(MapperOptions mapperOptions) {
        this.mapperOptions = mapperOptions;
        datastore = null;
        database = null;
        return this;
    }

    protected Version getServerVersion() {
        String version = (String) getMongoClient()
                .getDatabase("admin")
                .runCommand(new Document("serverStatus", 1))
                .get("version");
        return Version.valueOf(version);
    }

    public Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                .runCommand(new Document("ismaster", 1));
    }

    public MorphiaContainer start() {
        String mongodb = System.getProperty("mongodb");
        if ("local".equals(mongodb)) {
            LOG.info("'local' mongodb property specified. Using local server.");
            connectionString = "mongodb://localhost:27017/" + TEST_DB_NAME;
        } else {
            DockerImageName imageName;
            try {
                Versions match = mongodb == null
                        ? Versions.latest()
                        : Versions.bestMatch(mongodb);
                imageName = match.dockerImage();
            } catch (IllegalArgumentException e) {
                imageName = Versions.latest().dockerImage();
                LOG.error(format("Could not parse mongo docker image name.  using docker image %s.", imageName));
            }

            LOG.info("Running tests using " + imageName);
            mongoDBContainer = new MongoDBContainer(imageName);
            if (sharded) {
                mongoDBContainer
                        .withSharding();
            }
            mongoDBContainer.start();
            connectionString = mongoDBContainer.getReplicaSetUrl(TEST_DB_NAME);
        }
        return this;
    }

    public DatastoreImpl getDs() {
        if (datastore == null) {
            datastore = (DatastoreImpl) Morphia.createDatastore(getMongoClient(), TEST_DB_NAME, mapperOptions);
        }
        return datastore;

    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getDs().getDatabase();
        }
        return database;
    }

    protected MongoClient getMongoClient() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(builder()
                    .uuidRepresentation(mapperOptions.getUuidRepresentation())
                    .applyConnectionString(new ConnectionString(connectionString))
                    .build());

        }
        return mongoClient;
    }

    @Override
    public void close() {
        if (mongoDBContainer != null) {
            mongoDBContainer.close();
        }
    }
}
