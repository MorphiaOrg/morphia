package dev.morphia.test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import dev.morphia.DatastoreImpl;
import dev.morphia.config.MorphiaConfig;

import org.bson.Document;
import org.semver4j.Semver;

public class MorphiaContainer {
    private DatastoreImpl datastore;
    private MongoDatabase database;
    private final MorphiaConfig morphiaConfig;
    private final MongoClient mongoClient;

    public MorphiaContainer(MongoClient mongoClient, MorphiaConfig config) {
        this.morphiaConfig = config;
        this.mongoClient = mongoClient;
    }

    public MorphiaConfig getMorphiaConfig() {
        return morphiaConfig;
    }

    public void reset() {
        database = null;
        datastore = null;
    }

    protected Semver getServerVersion() {
        String version = (String) mongoClient
                .getDatabase("admin")
                .runCommand(new Document("serverStatus", 1))
                .get("version");
        return Semver.parse(version);
    }

    public Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                .runCommand(new Document("ismaster", 1));
    }

    public DatastoreImpl getDs() {
        if (datastore == null) {
            datastore = new DatastoreImpl(mongoClient, morphiaConfig);
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
