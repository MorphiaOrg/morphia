package dev.morphia;

import com.mongodb.client.MongoClient;

import dev.morphia.config.MorphiaConfig;

/**
 * Entry point for working with Morphia
 */
public final class Morphia {

    private Morphia() {
    }

    /**
     * Creates a Datastore configured via config file
     *
     * @param mongoClient the client to use
     * @return a Datastore that you can use to interact with MongoDB
     * @since 2.4.0
     */
    public static Datastore createDatastore(MongoClient mongoClient) {
        return new MorphiaDatastore(mongoClient, MorphiaConfig.load());
    }

    /**
     * Creates a Datastore configured via config file using the given classloader for resource and class resolution.
     *
     * @param mongoClient the client to use
     * @param classLoader the classloader to use
     * @return a Datastore that you can use to interact with MongoDB
     * @since 3.0
     */
    public static Datastore createDatastore(MongoClient mongoClient, ClassLoader classLoader) {
        return new MorphiaDatastore(mongoClient, MorphiaConfig.load(classLoader), classLoader);
    }

    /**
     * Creates a Datastore configured via config file
     *
     * @param mongoClient the client to use
     * @param config      the configuration to use
     * @return a Datastore that you can use to interact with MongoDB
     * @since 3.0.0
     */
    public static Datastore createDatastore(MongoClient mongoClient, MorphiaConfig config) {
        return new MorphiaDatastore(mongoClient, config);
    }

    /**
     * Creates a Datastore with the given configuration and classloader.
     *
     * @param mongoClient the client to use
     * @param config      the configuration to use
     * @param classLoader the classloader to use for class and resource resolution
     * @return a Datastore that you can use to interact with MongoDB
     * @since 3.0
     */
    public static Datastore createDatastore(MongoClient mongoClient, MorphiaConfig config, ClassLoader classLoader) {
        return new MorphiaDatastore(mongoClient, config, classLoader);
    }

}
