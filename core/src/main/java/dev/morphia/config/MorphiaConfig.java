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
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.QueryFactory;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * @since 2.4
 * @morphia.experimental
 */
@MorphiaExperimental
@ConfigMapping(prefix = "morphia")
public interface MorphiaConfig {

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
    boolean applyCaps();

    /**
     * If true, document validations will be enabled for entities/collections with such mapping.
     *
     * @mongodb.driver.manual core/document-validation/
     * @return true if the validations should be applied
     * @see Validation
     */
    @WithDefault("false")
    boolean applyDocumentValidations();

    /**
     * If true, mapped indexes will be applied to the database at start up.
     *
     * @return true if the indexes should be applied
     */
    @WithDefault("false")
    boolean applyIndexes();

    /**
     * Specifies a {@code CodecProvider} to supply user defined codecs that Morphia should use.
     *
     * @return the user configured CodecProvider
     * @see CodecProvider
     * @since 2.4
     * @deprecated this configuration entry will updated to use SPI as with other customizations
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
    boolean enablePolymorphicQueries();

    /**
     * Instructs Morphia to ignore final fields.
     *
     * @return true if Morphia should ignore final fields
     */
    @WithDefault("false")
    boolean ignoreFinals();

    /**
     * A comma delimited list of packages that Morphia should map.
     *
     * @return the list of packages, if any, to scan for entities to map
     * @see #mapSubpackages()
     */
    List<String> mapPackages();

    /**
     * Instructs Morphia to scan subpackages when mapping by package
     *
     * @return true if Morphia should map classes from the subpackages as well
     */
    @WithDefault("false")
    boolean mapSubpackages();

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
    boolean storeEmpties();

    /**
     * Instructs Morphia on how to handle null property values.
     *
     * @return true if Morphia should store null values
     */
    @WithDefault("false")
    boolean storeNulls();

    /**
     * @return the UUID representation to use in the driver
     * @deprecated This should be configured in the MongoClient given to Morphia
     */
    @WithDefault("standard")
    @Deprecated(forRemoval = true, since = "2.4.0")
    UuidRepresentation uuidRepresentation();
}
