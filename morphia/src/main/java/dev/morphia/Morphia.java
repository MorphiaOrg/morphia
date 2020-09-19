package dev.morphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.mapping.MapperOptions;


/**
 * Entry point for working with Morphia
 */
public final class Morphia {
    private Morphia() {
    }

    /**
     * Creates a Datastore
     *
     * @param dbName the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(String dbName) {
        return createDatastore(dbName, MapperOptions.DEFAULT);
    }

    /**
     * Creates a Datastore
     *
     * @param dbName  the name of the database
     * @param options the mapping options to use.
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(String dbName, MapperOptions options) {
        return createDatastore(MongoClients.create(MongoClientSettings.builder()
                                                                      .uuidRepresentation(options.getUuidRepresentation())
                                                                      .build()), dbName, options);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the client to use
     * @param dbName the name of the database
     * @param options the mapping options to use.
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient, String dbName, MapperOptions options) {
        return new DatastoreImpl(mongoClient, options, dbName);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the client to use
     * @param dbName the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient, String dbName) {
        return createDatastore(mongoClient, dbName, MapperOptions.DEFAULT);
    }
}
