package dev.morphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.config.MapperOptionsWrapper;
import dev.morphia.mapping.MapperOptions;

import static dev.morphia.config.MorphiaConfigHelper.*;

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
     * @deprecated use {@link #createDatastore(MongoClient)} and provide a configuration file instead. See the website docs for more detail
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
     * @param dbName      the name of the database
     * @param options     the mapping options to use.
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient, String dbName, MapperOptions options) {
        return new DatastoreImpl(mongoClient, new MapperOptionsWrapper(options, dbName));
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the client to use
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient, String dbName) {
        return createDatastore(mongoClient, dbName, MapperOptions.DEFAULT);
    }

    /**
     * Creates a Datastore configured via config file. If no config file exists, then defaults will be applied as defined in the
     * {@link dev.morphia.config.MorphiaConfig} interface.
     *
     * @param mongoClient the client to use
     * @return a Datastore that you can use to interact with MongoDB
     * @since 2.4.0
     */
    public static Datastore createDatastore(MongoClient mongoClient) {
        return new DatastoreImpl(mongoClient, loadConfig());
    }
}
