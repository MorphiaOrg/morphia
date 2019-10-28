package dev.morphia;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.mapping.MapperOptions;


public final class Morphia {
    private Morphia() {}

    /**
     * Creates a Datastore
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final String dbName) {
        return createDatastore(dbName, MapperOptions.DEFAULT);
    }

    /**
     * Creates a Datastore
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final String dbName, final MapperOptions options) {
        return createDatastore(MongoClients.create(), dbName, options);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final MongoClient mongoClient, final String dbName) {
        return createDatastore(mongoClient, dbName, MapperOptions.DEFAULT);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final MongoClient mongoClient, final String dbName, final MapperOptions options) {
        return new DatastoreImpl(mongoClient, options, dbName);
    }
}
