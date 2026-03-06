package dev.morphia.config;

import java.util.List;
import java.util.Optional;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.PossibleValues;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.converters.CodecProviderConverter;
import dev.morphia.config.converters.DiscriminatorFunctionConverter;
import dev.morphia.config.converters.NamingStrategyConverter;
import dev.morphia.config.converters.PropertyAnnotationProviderConverter;
import dev.morphia.config.converters.QueryFactoryConverter;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.query.QueryFactory;
import dev.morphia.sofia.Sofia;

import org.bson.codecs.configuration.CodecProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

import static io.smallrye.config.PropertiesConfigSourceLoader.*;
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

    /**
     * Tries to load a configuration from the default location.
     *
     * @return the loaded config
     */
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
        List<ConfigSource> configSources = inClassPath(location, 0, currentThread().getContextClassLoader());
        if (configSources.isEmpty()) {
            Sofia.logMissingConfigFile(location);
            return new ManualMorphiaConfig();
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
    MorphiaConfig collectionNaming(NamingStrategy value);

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
    MorphiaConfig applyCaps(Boolean value);

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
    MorphiaConfig applyDocumentValidations(Boolean value);

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
    MorphiaConfig applyIndexes(Boolean value);

    /**
     * Specifies a {@code CodecProvider} to supply user defined codecs that Morphia should use.
     *
     * @return the user configured CodecProvider
     * @see CodecProvider
     * @since 2.4
     */
    @WithConverter(CodecProviderConverter.class)
    Optional<CodecProvider> codecProvider();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    MorphiaConfig codecProvider(CodecProvider value);

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
    MorphiaConfig database(String value);

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
    MorphiaConfig dateStorage(DateStorage value);

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
    MorphiaConfig discriminator(DiscriminatorFunction value);

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
    MorphiaConfig discriminatorKey(String value);

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
    MorphiaConfig enablePolymorphicQueries(Boolean value);

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
    MorphiaConfig ignoreFinals(Boolean value);

    /**
     * Creates a new configuration based on the current one but updated to reflect the legacy configuration. This configuration is not
     * changed.
     *
     * @return the update configuration
     *
     * @since 3.0
     */
    MorphiaConfig legacy();

    /**
     * The mapper implementation to use. Defaults to {@link MapperType#LEGACY} (reflection-based).
     * Set to {@link MapperType#CRITTER} to use the bytecode-generated mapper (requires critter dependencies).
     *
     * @return the mapper type to use
     * @since 3.0
     */
    @WithDefault("legacy")
    MapperType mapper();

    /**
     * Updates this configuration with a new value and returns a new instance. The original instance is unchanged.
     *
     * @param value the new value
     * @return a new instance with the updated configuration
     * @since 3.0
     */
    MorphiaConfig mapper(MapperType value);

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
    MorphiaConfig packages(List<String> value);

    /**
     * Specifies the providers of any external annotations to use as markers for properties for Morphia to consider while mapping. This
     * method is marked as internal only to note that, as a relatively lower level hook in to Morphia functionality, the regular
     * guarantees about stability do not apply. This property and its underlying types and behaviors are subject to potential change
     * based on the evolving needs of Morphia internals. Users who need it are encouraged to use it with that caveat in mind.
     *
     * Morphia annotations will, of course, always be used regardless of any additional types specified.
     *
     * @return the list of providers
     * @since 3.0
     * @morphia.experimental
     * @morphia.internal
     *
     * @see PropertyAnnotationProvider
     */
    @MorphiaInternal
    @MorphiaExperimental
    @WithConverter(PropertyAnnotationProviderConverter.class)
    @WithDefault("dev.morphia.config.MorphiaPropertyAnnotationProvider")
    List<PropertyAnnotationProvider<?>> propertyAnnotationProviders();

    /**
     * Updates this configuration to include the new annotation providers for property discovery. If the default morphia provider is not
     * included it will be added.
     *
     * @param list the new providers
     *
     * @return this
     * @since 3.0
     * @morphia.internal
     * @morphia.experimental
     * @see #propertyAnnotationProviders()
     */
    @MorphiaInternal
    @MorphiaExperimental
    MorphiaConfig propertyAnnotationProviders(List<PropertyAnnotationProvider<?>> list);

    /**
     * Determines how properties are discovered. The traditional value is by scanning for fields which involves a bit more reflective
     * work. Alternately, scanning can check for get/set method pairs to determine which class properties should be mapped.
     *
     * @return the discovery method to use
     * @see PropertyDiscovery
     */
    @Deprecated
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
    MorphiaConfig propertyDiscovery(PropertyDiscovery value);

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
    MorphiaConfig propertyNaming(NamingStrategy value);

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
    MorphiaConfig queryFactory(QueryFactory value);

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
    MorphiaConfig storeEmpties(Boolean value);

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
    MorphiaConfig storeNulls(Boolean value);

    /**
     * Converts this instance in to the format needed for a configuration file
     *
     * @param showComplete true if all the entries should be shown. If false, only those settings with non-default values will be listed
     * @return the config file contents
     */
    String toConfigFormat(boolean showComplete);

}
