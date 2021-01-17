package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.conventions.ConfigureProperties;
import dev.morphia.mapping.conventions.FieldDiscovery;
import dev.morphia.mapping.conventions.MethodDiscovery;
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.query.QueryFactory;
import org.bson.UuidRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.morphia.mapping.MapperOptions.PropertyDiscovery.FIELDS;
import static org.bson.UuidRepresentation.STANDARD;

/**
 * Options to control mapping behavior.
 */
public class MapperOptions {
    public static final MapperOptions DEFAULT = MapperOptions.builder().build();
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);

    private final boolean ignoreFinals;
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final boolean cacheClassLookups;
    private final boolean mapSubPackages;
    private final DateStorage dateStorage;
    private final MorphiaInstanceCreator creator;
    private final String discriminatorKey;
    private final DiscriminatorFunction discriminator;
    private final List<MorphiaConvention> conventions;
    private final NamingStrategy collectionNaming;
    private final NamingStrategy propertyNaming;
    private final UuidRepresentation uuidRepresentation;
    private final QueryFactory queryFactory;
    private final boolean enablePolymorphicQueries;
    private ClassLoader classLoader;

    private MapperOptions(Builder builder) {
        cacheClassLookups = builder.cacheClassLookups();
        classLoader = builder.classLoader();
        collectionNaming = builder.collectionNaming();
        conventions = builder.conventions();
        creator = builder.creator();
        dateStorage = builder.dateStorage();
        discriminator = builder.discriminator();
        discriminatorKey = builder.discriminatorKey();
        enablePolymorphicQueries = builder.enablePolymorphicQueries();
        propertyNaming = builder.propertyNaming();
        ignoreFinals = builder.ignoreFinals();
        mapSubPackages = builder.mapSubPackages();
        queryFactory = builder.queryFactory();
        storeEmpties = builder.storeEmpties();
        storeNulls = builder.storeNulls();
        uuidRepresentation = builder.uuidRepresentation();
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param original an existing set of options to use as a starting point
     * @return a builder to set mapping options
     */
    public static Builder builder(MapperOptions original) {
        Builder builder = new Builder();
        builder.ignoreFinals = original.isIgnoreFinals();
        builder.storeNulls = original.isStoreNulls();
        builder.storeEmpties = original.isStoreEmpties();
        builder.cacheClassLookups = original.isCacheClassLookups();
        builder.mapSubPackages = original.isMapSubPackages();
        builder.creator = original.getCreator();
        builder.classLoader = original.getClassLoader();
        builder.dateStorage = original.getDateStorage();
        return builder;
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder legacy() {
        return new Builder()
                   .dateStorage(DateStorage.SYSTEM_DEFAULT)
                   .discriminatorKey("className")
                   .discriminator(DiscriminatorFunction.className())
                   .collectionNaming(NamingStrategy.identity())
                   .propertyNaming(NamingStrategy.identity())
                   .queryFactory(new LegacyQueryFactory());
    }

    /**
     * Returns the classloader used, in theory, when loading the entity types.
     *
     * @return the classloader
     * @morphia.internal
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    /**
     * @return the naming strategy for collections unless explicitly set via @Entity
     * @see Entity
     */
    public NamingStrategy getCollectionNaming() {
        return collectionNaming;
    }

    /**
     * @return the configured Conventions
     */
    public List<MorphiaConvention> getConventions() {
        return Collections.unmodifiableList(conventions);
    }

    /**
     * @return the factory to use when creating new instances
     */
    public MorphiaInstanceCreator getCreator() {
        return creator;
    }

    /**
     * @return the date storage configuration value
     */
    public DateStorage getDateStorage() {
        return dateStorage;
    }

    /**
     * @return the function to determine discriminator value
     */
    public DiscriminatorFunction getDiscriminator() {
        return discriminator;
    }

    /**
     * @return the discriminator property name
     */
    public String getDiscriminatorKey() {
        return discriminatorKey;
    }

    /**
     * @return the naming strategy for properties unless explicitly set via @Property
     * @see Property
     * @deprecated use {@link #getPropertyNaming()} instead
     */
    @Deprecated(forRemoval = true)
    public NamingStrategy getFieldNaming() {
        return getPropertyNaming();
    }

    /**
     * @return the naming strategy for properties unless explicitly set via @Property
     * @see Property
     * @since 2.2
     */
    public NamingStrategy getPropertyNaming() {
        return propertyNaming;
    }

    /**
     * @return the query factory used by the Datastore
     * @since 2.0
     */
    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

    /**
     * @return the UUID representation to use in the driver
     */
    public UuidRepresentation getUuidRepresentation() {
        return uuidRepresentation;
    }

    /**
     * @return true if Morphia should cache name to Class lookups
     */
    public boolean isCacheClassLookups() {
        return cacheClassLookups;
    }

    /**
     * @return true if polymorphic queries are enabled
     */
    public boolean isEnablePolymorphicQueries() {
        return enablePolymorphicQueries;
    }

    /**
     * @return true if Morphia should ignore final fields
     */
    public boolean isIgnoreFinals() {
        return ignoreFinals;
    }

    /**
     * @return true if Morphia should map classes from the sub-packages as well
     */
    public boolean isMapSubPackages() {
        return mapSubPackages;
    }

    /**
     * @return true if Morphia should store empty values for lists/maps/sets/arrays
     */
    public boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * @return true if Morphia should store null values
     */
    public boolean isStoreNulls() {
        return storeNulls;
    }

    public enum PropertyDiscovery {
        FIELDS,
        METHODS
    }

    /**
     * A builder class for setting mapping options
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        private final List<MorphiaConvention> conventions = new ArrayList<>();
        private boolean ignoreFinals;
        private boolean storeNulls;
        private boolean storeEmpties;
        private boolean cacheClassLookups;
        private boolean mapSubPackages;
        private boolean enablePolymorphicQueries;
        private MorphiaInstanceCreator creator;
        private ClassLoader classLoader;
        private DateStorage dateStorage = DateStorage.UTC;
        private String discriminatorKey = "_t";
        private DiscriminatorFunction discriminator = DiscriminatorFunction.simpleName();
        private NamingStrategy collectionNaming = NamingStrategy.camelCase();
        private NamingStrategy propertyNaming = NamingStrategy.identity();
        private UuidRepresentation uuidRepresentation = STANDARD;
        private QueryFactory queryFactory = new DefaultQueryFactory();
        private PropertyDiscovery propertyDiscovery = FIELDS;

        private Builder() {
        }

        /**
         * Adds a custom convention to the list to be applied to all new MorphiaModels.
         *
         * @param convention the new convention
         * @return this
         * @since 2.0
         */
        public Builder addConvention(MorphiaConvention convention) {
            conventions.add(convention);

            return this;
        }

        /**
         * @return the new options instance
         */
        public MapperOptions build() {
            return new MapperOptions(this);
        }

        /**
         * @param cacheClassLookups if true class lookups are cached
         * @return this
         */
        public Builder cacheClassLookups(boolean cacheClassLookups) {
            this.cacheClassLookups = cacheClassLookups;
            return this;
        }

        /**
         * @param classLoader the ClassLoader to use
         * @return this
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Sets the naming strategy to use for collection names
         *
         * @param strategy the strategy to use
         * @return this
         */
        public Builder collectionNaming(NamingStrategy strategy) {
            this.collectionNaming = strategy;
            return this;
        }

        /**
         * @param dateStorage the storage format to use for dates
         * @return this
         * @deprecated use {@link #dateStorage(DateStorage)} instead.
         */
        @Deprecated
        public Builder dateForm(DateStorage dateStorage) {
            return dateStorage(dateStorage);
        }

        /**
         * The default value for this is {@link DateStorage#UTC}.  To use the {@link DateStorage#SYSTEM_DEFAULT}, either set this value
         * explicitly here or use the {@link #legacy()} Builder.
         *
         * @param dateStorage the storage format to use for dates
         * @return this
         * @since 2.0
         */
        public Builder dateStorage(DateStorage dateStorage) {
            this.dateStorage = dateStorage;
            return this;
        }

        /**
         * @param disableEmbeddedIndexes if true scanning @Embedded properties for indexing is disabled
         * @return this
         * @deprecated unused
         */
        @Deprecated(forRemoval = false)
        public Builder disableEmbeddedIndexes(boolean disableEmbeddedIndexes) {
            LOG.warn("this option is no longer used");
            return this;
        }

        /**
         * Sets the discriminator function to use
         *
         * @param function the function to use
         * @return this
         */
        public Builder discriminator(DiscriminatorFunction function) {
            this.discriminator = function;
            return this;
        }

        /**
         * Defines the discriminator key name
         *
         * @param key the key to use, e.g., "_t".  the default/legacy value is "className"
         * @return this
         */
        public Builder discriminatorKey(String key) {
            this.discriminatorKey = key;
            return this;
        }

        /**
         * @param enablePolymorphicQueries if true queries are updated, in some cases, to check for subtypes' discriminator values so
         *                                 that subtype might be returned by a query.
         * @return this
         */
        public Builder enablePolymorphicQueries(boolean enablePolymorphicQueries) {
            this.enablePolymorphicQueries = enablePolymorphicQueries;
            return this;
        }

        /**
         * Sets the naming strategy to use for fields unless expliclity set via @Property
         *
         * @param strategy the strategy to use
         * @return this
         * @see Property
         * @deprecated use {@link #propertyNaming(NamingStrategy)} instead
         */
        @Deprecated(forRemoval = true)
        public Builder fieldNaming(NamingStrategy strategy) {
            return propertyNaming(strategy);
        }

        /**
         * @param ignoreFinals if true final fields are ignored
         * @return this
         */
        public Builder ignoreFinals(boolean ignoreFinals) {
            this.ignoreFinals = ignoreFinals;
            return this;
        }

        /**
         * @param mapSubPackages if true subpackages are mapped when given a particular package
         * @return this
         */
        public Builder mapSubPackages(boolean mapSubPackages) {
            this.mapSubPackages = mapSubPackages;
            return this;
        }

        /**
         * @param creator the object factory to use when creating instances
         * @return this
         */
        public Builder objectFactory(MorphiaInstanceCreator creator) {
            this.creator = creator;
            return this;
        }

        /**
         * Determines how properties are discovered on mapped entities
         *
         * @param discovery
         * @return this
         * @since 2.2
         */
        public Builder propertyDiscovery(PropertyDiscovery discovery) {
            this.propertyDiscovery = discovery;
            return this;
        }

        /**
         * Sets the naming strategy to use for propertys unless expliclity set via @Property
         *
         * @param strategy the strategy to use
         * @return this
         * @see Property
         * @since 2.2
         */
        public Builder propertyNaming(NamingStrategy strategy) {
            this.propertyNaming = strategy;
            return this;
        }

        /**
         * @param queryFactory the query factory to use when creating queries
         * @return this
         */
        public Builder queryFactory(QueryFactory queryFactory) {
            this.queryFactory = queryFactory;
            return this;
        }

        /**
         * @param storeEmpties if true empty maps and collection types are stored in the database
         * @return this
         */
        public Builder storeEmpties(boolean storeEmpties) {
            this.storeEmpties = storeEmpties;
            return this;
        }

        /**
         * @param storeNulls if true null values are stored in the database
         * @return this
         */
        public Builder storeNulls(boolean storeNulls) {
            this.storeNulls = storeNulls;
            return this;
        }

        /**
         * @param useLowerCaseCollectionNames if true, generated collections names are lower cased
         * @return this
         * @deprecated use {@link #collectionNaming(NamingStrategy)} instead
         */
        @Deprecated(since = "2.0", forRemoval = true)
        public Builder useLowerCaseCollectionNames(boolean useLowerCaseCollectionNames) {
            if (useLowerCaseCollectionNames) {
                collectionNaming(NamingStrategy.lowerCase());
            }
            return this;
        }

        /**
         * Configures the UUID representation to use
         *
         * @param uuidRepresentation the representation
         * @return this
         */
        public Builder uuidRepresentation(UuidRepresentation uuidRepresentation) {
            this.uuidRepresentation = uuidRepresentation;
            return this;
        }

        private boolean cacheClassLookups() {
            return cacheClassLookups;
        }

        private ClassLoader classLoader() {
            return classLoader;
        }

        private NamingStrategy collectionNaming() {
            return collectionNaming;
        }

        private List<MorphiaConvention> conventions() {
            if (conventions.isEmpty()) {
                return List.of(
                    new MorphiaDefaultsConvention(),
                    propertyDiscovery == FIELDS ? new FieldDiscovery() : new MethodDiscovery(),
                    new ConfigureProperties());
            }
            return conventions;
        }

        private MorphiaInstanceCreator creator() {
            return creator;
        }

        private DateStorage dateStorage() {
            return dateStorage;
        }

        private DiscriminatorFunction discriminator() {
            return discriminator;
        }

        private String discriminatorKey() {
            return discriminatorKey;
        }

        private boolean enablePolymorphicQueries() {
            return enablePolymorphicQueries;
        }

        private boolean ignoreFinals() {
            return ignoreFinals;
        }

        private boolean mapSubPackages() {
            return mapSubPackages;
        }

        private PropertyDiscovery propertyDiscovery() {
            return propertyDiscovery;
        }

        private NamingStrategy propertyNaming() {
            return propertyNaming;
        }

        private QueryFactory queryFactory() {
            return queryFactory;
        }

        private boolean storeEmpties() {
            return storeEmpties;
        }

        private boolean storeNulls() {
            return storeNulls;
        }

        private UuidRepresentation uuidRepresentation() {
            return uuidRepresentation;
        }
    }
}
