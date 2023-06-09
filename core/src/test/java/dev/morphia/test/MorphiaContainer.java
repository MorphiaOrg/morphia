package dev.morphia.test;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import dev.morphia.DatastoreImpl;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.Document;

@SuppressWarnings("removal")
public class MorphiaContainer {
    private DatastoreImpl datastore;
    private MongoDatabase database;
    private final MorphiaConfig morphiaConfig;
    private final MongoClient mongoClient;

    public MorphiaContainer(MongoClient mongoClient, MorphiaConfig config) {
        this.morphiaConfig = config;
        this.mongoClient = mongoClient;
    }

    public void reset() {
        database = null;
        datastore = null;
    }

    protected Version getServerVersion() {
        String version = (String) mongoClient
                .getDatabase("admin")
                .runCommand(new Document("serverStatus", 1))
                .get("version");
        return Version.valueOf(version);
    }

    public Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                .runCommand(new Document("ismaster", 1));
    }

    public DatastoreImpl getDs() {
        if (datastore == null) {
            datastore = (DatastoreImpl) new DatastoreImpl(new Mapper(morphiaConfig), mongoClient, morphiaConfig.database());
        }
        return datastore;
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getDs().getDatabase();
        }
        return database;
    }
}
