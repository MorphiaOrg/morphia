package dev.morphia.config;

import java.util.List;
import java.util.Optional;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.QueryFactory;
import dev.morphia.sofia.Sofia;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

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
     * The database name that Morphia should use. This entry is required to be present and is the only necessary configuration element
     * you need to provide as all the other entries have discernible default values.
     *
     * @return the database name to be used with this configuration
     */
    String database();

    /**
     * If true, collection caps will be applied to the database at start up.
     *
     * @return true if the caps should be applied
     */
    @WithDefault("false")
    Boolean applyCaps();

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
     * If true, mapped indexes will be applied to the database at start up.
     *
     * @return true if the indexes should be applied
     */
    @WithDefault("false")
    Boolean applyIndexes();

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
    @PossibleValues({ "camelCase", "identity", "kebabCase", "lowerCase", "snakeCase" })
    @WithConverter(NamingStrategyConverter.class)
    NamingStrategy collectionNaming();

    /**
     * The date storage configuration Morphia should use for JSR 310 types.
     *
     * @return the date storage configuration value
     */
    @WithDefault("utc")
    DateStorage dateStorage();

    /**
     * The function to use when calculating the discriminator value for an entity
     *
     * @return the function to use
     * @see DiscriminatorFunction
     */
    @WithDefault("simpleName")
    @WithConverter(DiscriminatorFunctionConverter.class)
    @PossibleValues({ "className", "lowerClassName", "lowerSimpleName", "simpleName" })
    DiscriminatorFunction discriminator();

    /**
     * The document field name to use when storing discriminator values
     *
     * @return the discriminator property name
     */
    @WithDefault("_t")
    String discriminatorKey();

    /**
     * Enable polymorphic queries. By default, Morphia will only query for the given type. However, in cases where subtypes are stored
     * in the same location, enabling this feature will instruct Morphia to fetch any subtypes that satisfy the query elements.
     *
     * @return true if polymorphic queries are enabled
     */
    @WithDefault("false")
    Boolean enablePolymorphicQueries();

    /**
     * Instructs Morphia to ignore final fields.
     *
     * @return true if Morphia should ignore final fields
     */
    @WithDefault("false")
    Boolean ignoreFinals();

    /**
     * A comma delimited list of packages that Morphia should map.
     *
     * @return the list of packages to scan for entities
     */
    List<String> packages();

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
    @PossibleValues({ "camelCase", "identity", "kebabCase", "lowerCase", "snakeCase" })
    NamingStrategy propertyNaming();

    /**
     * Specifies the query factory to use. Typically, there is no need to set this value.
     *
     * @return the query factory
     */
    @WithConverter(QueryFactoryConverter.class)
    @WithDefault("dev.morphia.query.DefaultQueryFactory")
    QueryFactory queryFactory();

    /**
     * Instructs Morphia on how to handle empty Collections and Maps.
     *
     * @return true if Morphia should store empty values for lists/maps/sets/arrays
     */
    @WithDefault("false")
    Boolean storeEmpties();

    /**
     * Instructs Morphia on how to handle null property values.
     *
     * @return true if Morphia should store null values
     */
    @WithDefault("false")
    Boolean storeNulls();

    /**
     * @return the UUID representation to use in the driver
     * @deprecated This should be configured in the MongoClient given to Morphia
     */
    @WithDefault("standard")
    @Deprecated(forRemoval = true, since = "2.4.0")
    UuidRepresentation uuidRepresentation();
}
