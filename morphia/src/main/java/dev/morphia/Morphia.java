package dev.morphia;

import com.mongodb.MongoClient;
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
        return createDatastore(new MongoClient(), dbName);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final MongoClient mongoClient, final String dbName) {
        return new DatastoreImpl(mongoClient, MapperOptions.builder().build(), dbName);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(final MongoClient mongoClient, final MapperOptions options, final String dbName) {
        return new DatastoreImpl(mongoClient, options, dbName);
    }
}
