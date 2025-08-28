package dev.morphia.test;

import com.mongodb.client.MongoDatabase;
import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

/**
 * Provides scoped access to a temporary MorphiaContainer with custom configuration.
 * This allows test methods to use different configurations without affecting the global test setup.
 */
public class ConfigScope {

    private static final ThreadLocal<ConfigScope> CURRENT_SCOPE = new ThreadLocal<>();

    private final MorphiaContainer tempContainer;

    ConfigScope(MorphiaContainer tempContainer) {
        this.tempContainer = tempContainer;
    }

    /**
     * Sets the current scope for this thread.
     *
     * @param scope the scope to set
     */
    static void setCurrentScope(ConfigScope scope) {
        CURRENT_SCOPE.set(scope);
    }

    /**
     * Clears the current scope for this thread.
     */
    static void clearCurrentScope() {
        CURRENT_SCOPE.remove();
    }

    /**
     * Gets the current scope for this thread.
     *
     * @return the current scope, or null if none is set
     */
    public static ConfigScope getCurrentScope() {
        return CURRENT_SCOPE.get();
    }

    /**
     * Gets the MorphiaDatastore for this configuration scope.
     *
     * @return the temporary datastore with custom configuration
     */
    public MorphiaDatastore getDs() {
        return tempContainer.getDs();
    }

    /**
     * Gets the MongoDB database for this configuration scope.
     *
     * @return the database instance
     */
    public MongoDatabase getDatabase() {
        return tempContainer.getDatabase();
    }

    /**
     * Gets the Mapper for this configuration scope.
     *
     * @return the mapper with custom configuration
     */
    public Mapper getMapper() {
        return tempContainer.getDs().getMapper();
    }

    /**
     * Gets the MorphiaConfig for this configuration scope.
     *
     * @return the custom configuration
     */
    public MorphiaConfig getConfig() {
        return tempContainer.getMorphiaConfig();
    }

    /**
     * Gets the underlying MorphiaContainer.
     *
     * @return the temporary container
     */
    public MorphiaContainer getContainer() {
        return tempContainer;
    }
}