package dev.morphia;

import java.util.Map;
import java.util.TreeMap;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;

import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableMap;

/**
 * Entry point for working with Morphia
 */
public final class Morphia {
    public static final String MORPHIA_CONFIG_PROPERTIES = "META-INF/morphia-config.properties";

    private Morphia() {
    }

    /**
     * Creates a Datastore
     *
     * @param dbName the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     * @deprecated Please use {@link #createDatastore(MongoClient, String)}
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
     * @deprecated Please use {@link #createDatastore(MongoClient, String, MapperOptions)}
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
     */
    public static Datastore createDatastore(MongoClient mongoClient, String dbName, MapperOptions options) {
        return new DatastoreImpl(new Mapper(options), mongoClient, dbName);
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
     * Creates a Datastore configured via config file
     *
     * @param mongoClient the client to use
     * @return a Datastore that you can use to interact with MongoDB
     */
    public static Datastore createDatastore(MongoClient mongoClient) {
        Config config = ConfigProvider.getConfig();
        SmallRyeConfig smallRyeConfig = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MorphiaConfig.class)
                .withSources(new EnvConfigSource(getEnvProperties(), 300),
                        new SysPropConfigSource())
                .withSources(classPathSources(MORPHIA_CONFIG_PROPERTIES, currentThread().getContextClassLoader()))
                .addDefaultSources()
                .build();
        MorphiaConfig configMapping = smallRyeConfig.getConfigMapping(MorphiaConfig.class);
        System.out.println("configMapping.database() = " + configMapping.database());
        System.out.println("configMapping = " + configMapping);
        return null; //createDatastore(mongoClient, dbName, MapperOptions.DEFAULT);
    }

    private static Map<String, String> getEnvProperties() {
        return unmodifiableMap(new TreeMap<>(System.getenv()));
    }

}
