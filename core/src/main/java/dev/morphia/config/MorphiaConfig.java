package dev.morphia.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.query.QueryFactory;
import dev.morphia.sofia.Sofia;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import static dev.morphia.config.MorphiaConfigHelper.dumpConfigurationFile;
import static dev.morphia.mapping.DiscriminatorFunction.className;
import static dev.morphia.mapping.NamingStrategy.identity;
import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static java.lang.Thread.currentThread;

/**
 * Please note that there is every expectation that this format/naming is stable. However, based on usage feedback prior to 3.0 some
 * tweaks might be made to improve the experience. As of 3.0, the experimental label will be dropped and the format fixed for the
 * existing configuration values.
 *
 * @since 2.4
 * @morphia.experimental
 */
@MorphiaExperimental
@ConfigMapping(prefix = "morphia")
public interface MorphiaConfig {

    static MorphiaConfig load() {
        return load(MorphiaConfigHelper.MORPHIA_CONFIG_PROPERTIES);
    }

    /**
     * Parses and loads the configuration found at the given location
     *
     * @param location the location of the configuration to load. This can be a file path, a classpath resource, a URL, etc.
     *
     * @return the loaded configuration
     * @since 3.0
     */
    static MorphiaConfig load(String location) {
        List<ConfigSource> configSources = classPathSources(location, currentThread().getContextClassLoader());
        if (configSources.isEmpty()) {
            throw new MappingException(Sofia.missingConfigFile(location));
        }
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MorphiaConfig.class)
                .withSources(configSources)
                .addDefaultSources()
                .build()
                .getConfigMapping(MorphiaConfig.class);
    }

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig collectionNaming(NamingStrategy value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.collectionNaming = value;
        return newConfig;
    }

    /**
     * The database name that Morphia should use. This entry is required to be present and is the only necessary configuration element
     * you need to provide as all the other entries have discernible default values.
     *
     * @return the database name to be used with this configuration
     */
    @WithDefault("morphia")
    String database();

    /**
     * If true, collection caps will be applied to the database at start up.
     *
     * @return true if the caps should be applied
     */
    @WithDefault("false")
    Boolean applyCaps();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig applyCaps(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.applyCaps = value;
        return newConfig;
    }

    /**
     * If true, document validations will be enabled for entities/collections with validation mappings.
     *
     * @mongodb.driver.manual core/document-validation/
     * @return true if the validations should be applied
     * @see Validation
     */
    @WithDefault("false")
    Boolean applyDocumentValidations();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig applyDocumentValidations(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.applyDocumentValidations = value;
        return newConfig;
    }

    /**
     * If true, mapped indexes will be applied to the database at start up.
     *
     * @return true if the indexes should be applied
     */
    @WithDefault("false")
    Boolean applyIndexes();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig applyIndexes(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.applyIndexes = value;
        return newConfig;
    }

    /**
     * Specifies a {@code CodecProvider} to supply user defined codecs that Morphia should use.
     *
     * @return the user configured CodecProvider
     * @see CodecProvider
     * @since 2.4
     * @deprecated this configuration entry will be updated to use SPI as with other customizations
     */
    @Deprecated(since = "2.4.0", forRemoval = true)
    @WithConverter(CodecConverter.class)
    Optional<CodecProvider> codecProvider();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig codecProvider(CodecProvider value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.codecProvider = Optional.of(value);
        return newConfig;
    }

    /**
     * Sets the naming strategy to be used when generating collection names for entities if name is not explicitly given in the {@code
     * Entity} annotation
     * <p>
     * Possible values include the documented values below as well as the fully qualified class name of a user supplied strategy.
     *
     * @return the strategy to use
     * @see Entity
     * @see NamingStrategy
     */
    @WithDefault("camelCase")
    @PossibleValues({ "camelCase", "identity", "kebabCase", "lowerCase", "snakeCase", "fqcn" })
    @WithConverter(NamingStrategyConverter.class)
    NamingStrategy collectionNaming();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig database(String value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.database = value;
        return newConfig;
    }

    /**
     * The date storage configuration Morphia should use for JSR 310 types.
     *
     * @return the date storage configuration value
     */
    @WithDefault("utc")
    DateStorage dateStorage();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig dateStorage(DateStorage value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.dateStorage = value;
        return newConfig;
    }

    /**
     * The function to use when calculating the discriminator value for an entity
     *
     * @return the function to use
     * @see DiscriminatorFunction
     */
    @WithDefault("simpleName")
    @WithConverter(DiscriminatorFunctionConverter.class)
    @PossibleValues({ "className", "lowerClassName", "lowerSimpleName", "simpleName", "fqcn" })
    DiscriminatorFunction discriminator();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig discriminator(DiscriminatorFunction value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.discriminator = value;
        return newConfig;
    }

    /**
     * The document field name to use when storing discriminator values
     *
     * @return the discriminator property name
     */
    @WithDefault("_t")
    String discriminatorKey();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig discriminatorKey(String value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.discriminatorKey = value;
        return newConfig;
    }

    /**
     * Enable polymorphic queries. By default, Morphia will only query for the given type. However, in cases where subtypes are stored
     * in the same location, enabling this feature will instruct Morphia to fetch any subtypes that satisfy the query elements.
     *
     * @return true if polymorphic queries are enabled
     */
    @WithDefault("false")
    Boolean enablePolymorphicQueries();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig enablePolymorphicQueries(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.enablePolymorphicQueries = value;
        return newConfig;
    }

    /**
     * Instructs Morphia to ignore final fields.
     *
     * @return true if Morphia should ignore final fields
     */
    @WithDefault("false")
    Boolean ignoreFinals();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig ignoreFinals(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.ignoreFinals = value;
        return newConfig;
    }

    /**
     * Creates a new configuration based on the current one but updated to reflect the legacy configuration. This configuration is not
     * changed.
     * 
     * @return the update configuration
     *
     * @since 3.0
     */
    default MorphiaConfig legacy() {
        ManualMorphiaConfig newConfig = new ManualMorphiaConfig(this);
        newConfig.dateStorage = DateStorage.SYSTEM_DEFAULT;
        newConfig.discriminatorKey = "className";
        newConfig.discriminator = className();
        newConfig.collectionNaming = identity();
        newConfig.propertyNaming = identity();
        newConfig.queryFactory = new LegacyQueryFactory();

        return newConfig;

    }

    /**
     * A comma delimited list of packages that Morphia should map. If subpackages of a specific package should also be mapped, simply add
     * a '*' to the end of the package name. e.g., 'com.foo.bar.*'
     *
     * @return the list of packages to scan for entities
     */
    @WithDefault(".*")
    List<String> packages();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig packages(List<String> value) {
        var newConfig = new ManualMorphiaConfig(this);

        if (!value.isEmpty()) {
            newConfig.packages = new ArrayList<>(value);
        }
        return newConfig;
    }

    /**
     * Determines how properties are discovered. The traditional value is by scanning for fields which involves a bit more reflective
     * work. Alternately, scanning can check for get/set method pairs to determine which class properties should be mapped.
     *
     * @return the discovery method to use
     * @see PropertyDiscovery
     */
    @WithDefault("fields")
    @PossibleValues(value = { "fields", "methods" }, fqcn = false)
    PropertyDiscovery propertyDiscovery();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig propertyDiscovery(PropertyDiscovery value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.propertyDiscovery = value;
        return newConfig;
    }

    /**
     * Defines the strategy to use when generating property names to document field names for storage in the database when not explicitly
     * set using {@code Property}.
     * <p>
     * Possible values include the documented values below as well as the fully qualified class name of a user supplied strategy.
     *
     * @return the naming strategy for properties unless explicitly set via @Property
     * @see Property
     * @see NamingStrategy
     */
    @WithDefault("identity")
    @WithConverter(NamingStrategyConverter.class)
    @PossibleValues({ "camelCase", "identity", "kebabCase", "lowerCase", "snakeCase", "fqcn" })
    NamingStrategy propertyNaming();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig propertyNaming(NamingStrategy value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.propertyNaming = value;
        return newConfig;
    }

    /**
     * Specifies the query factory to use. Typically, there is no need to set this value.
     *
     * @return the query factory
     */
    @WithConverter(QueryFactoryConverter.class)
    @WithDefault("dev.morphia.query.DefaultQueryFactory")
    QueryFactory queryFactory();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig queryFactory(QueryFactory value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.queryFactory = value;
        return newConfig;
    }

    /**
     * Instructs Morphia on how to handle empty Collections and Maps.
     *
     * @return true if Morphia should store empty values for lists/maps/sets/arrays
     */
    @WithDefault("false")
    Boolean storeEmpties();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig storeEmpties(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.storeEmpties = value;
        return newConfig;
    }

    /**
     * Instructs Morphia on how to handle null property values.
     *
     * @return true if Morphia should store null values
     */
    @WithDefault("false")
    Boolean storeNulls();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig storeNulls(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.storeNulls = value;
        return newConfig;
    }

    /**
     * @return the UUID representation to use in the driver
     * @deprecated This should be configured in the MongoClient given to Morphia
     */
    @WithDefault("standard")
    @Deprecated(forRemoval = true, since = "2.4.0")
    UuidRepresentation uuidRepresentation();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    default MorphiaConfig uuidRepresentation(UuidRepresentation value) {
        var newConfig = new ManualMorphiaConfig(this);

        newConfig.uuidRepresentation = value;
        return newConfig;
    }

    /**
     * Converts this instance in to the format needed for a configuration file
     *
     * @param showComplete true if all the entries should be shown. If false, only those settings with non-default values will be listed
     * @return the config file contents
     */
    default String toConfigFormat(boolean showComplete) {
        return dumpConfigurationFile(this, showComplete);
    }

}
