package dev.morphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.config.MapperOptionsWrapper;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.config.MorphiaConfigHelper;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for working with Morphia
 */
public final class Morphia {

    private static final Logger LOG = LoggerFactory.getLogger(Morphia.class);

    private Morphia() {
    }

    /**
     * Creates a Datastore
     *
     * @param dbName the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     * @deprecated use {@link #createDatastore(MongoClient)} and provide a configuration file instead. See the website docs for more detail
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public static Datastore createDatastore(String dbName) {
        return createDatastore(dbName, MapperOptions.DEFAULT);
    }

    /**
     * Creates a Datastore
     *
     * @param dbName  the name of the database
     * @param options the mapping options to use.
     * @return a Datastore that you can use to interact with MongoDB
     * @deprecated use {@link #createDatastore(MongoClient)} and provide a configuration file instead. See the website docs for more detail
     */
    @Deprecated(forRemoval = true, since = "2.3")
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
     * @deprecated use {@link #createDatastore(MongoClient)} and provide a configuration file instead. See the website docs for more detail
     */
    @Deprecated(forRemoval = true, since = "2.4.0")
    public static Datastore createDatastore(MongoClient mongoClient, String dbName, MapperOptions options) {
        MapperOptionsWrapper config = new MapperOptionsWrapper(options, dbName);
        var configContents = MorphiaConfigHelper.dumpConfigurationFile(options, dbName, true);
        LOG.info("Morphia 3.0 will be moving to a configuration file based setup.  As such MapperOptions will be removed in the next " +
                "major release.  To remove this message, create the file 'META-INF/morphia-config.properties' in your resources folder " +
                "using the following text.  Entries with default values may be omitted but are included here for completeness.\n" +
                configContents);

        return new DatastoreImpl(new Mapper(config), mongoClient, dbName);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the client to use
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     * @deprecated use {@link #createDatastore(MongoClient)} and provide a configuration file instead. See the website docs for more detail
     */
    @Deprecated(forRemoval = true, since = "2.4.0")
    public static Datastore createDatastore(MongoClient mongoClient, String dbName) {
        return createDatastore(mongoClient, dbName, MapperOptions.DEFAULT);
    }

    /**
     * Creates a Datastore configured via config file
     *
     * @param mongoClient the client to use
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient) {
        MorphiaConfig configMapping = MorphiaConfigHelper.loadConfigMapping();
        return new DatastoreImpl(new Mapper(configMapping), mongoClient, configMapping.database());
    }

}
