package dev.morphia.mapping;


import dev.morphia.ObjectFactory;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.cache.DefaultEntityCacheFactory;
import dev.morphia.mapping.cache.EntityCacheFactory;
import dev.morphia.mapping.lazy.DatastoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options to control mapping behavior.
 */
@SuppressWarnings("deprecation")
public class MapperOptions {
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);
    private boolean ignoreFinals; //ignore final fields.
    private boolean storeNulls;
    private boolean storeEmpties;
    private boolean useLowerCaseCollectionNames;
    private boolean cacheClassLookups;
    private boolean mapSubPackages;
    private boolean disableEmbeddedIndexes;
    private boolean cachingEnabled;
    private DateStorage dateStorage = DateStorage.SYSTEM_DEFAULT;
    private ObjectFactory objectFactory;
    private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
    private CustomMapper embeddedMapper = new EmbeddedMapper();
    private CustomMapper defaultMapper = embeddedMapper;
    private CustomMapper referenceMapper = new ReferenceMapper();
    private CustomMapper valueMapper = new ValueMapper();
    private ClassLoader classLoader;

    /**
     * Creates a default options instance.
     *
     * @see #builder()
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public MapperOptions() {
    }

    /**
     * Copy Constructor
     *
     * @param options the MapperOptions to copy
     * @see #builder(MapperOptions)
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public MapperOptions(final MapperOptions options) {
        ignoreFinals = options.isIgnoreFinals();
        storeNulls = options.isStoreNulls();
        storeEmpties = options.isStoreEmpties();
        useLowerCaseCollectionNames = options.isUseLowerCaseCollectionNames();
        cacheClassLookups = options.isCacheClassLookups();
        objectFactory = options.getObjectFactory();
        cacheFactory = options.getCacheFactory();
        embeddedMapper = options.getEmbeddedMapper();
        defaultMapper = options.getDefaultMapper();
        referenceMapper = options.getReferenceMapper();
        valueMapper = options.getValueMapper();
        mapSubPackages = options.isMapSubPackages();
        dateStorage = options.dateStorage;
        disableEmbeddedIndexes = options.disableEmbeddedIndexes;
        classLoader = options.getClassLoader();
        cachingEnabled = options.cachingEnabled;
    }

    private MapperOptions(final Builder builder) {
        ignoreFinals = builder.ignoreFinals;
        disableEmbeddedIndexes = builder.disableEmbeddedIndexes;
        storeNulls = builder.storeNulls;
        storeEmpties = builder.storeEmpties;
        useLowerCaseCollectionNames = builder.useLowerCaseCollectionNames;
        cacheClassLookups = builder.cacheClassLookups;
        mapSubPackages = builder.mapSubPackages;
        objectFactory = builder.objectFactory;
        cacheFactory = builder.cacheFactory;
        embeddedMapper = builder.embeddedMapper;
        defaultMapper = builder.defaultMapper;
        referenceMapper = builder.referenceMapper;
        valueMapper = builder.valueMapper;
        dateStorage = builder.dateStorage;
        classLoader = builder.classLoader;
        cachingEnabled = builder.cachingEnabled;
    }

    /**
     * @return the factory to create an EntityCache
     */
    public EntityCacheFactory getCacheFactory() {
        return cacheFactory;
    }

    /**
     * Sets the factory to create an EntityCache
     *
     * @param cacheFactory the factory
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setCacheFactory(final EntityCacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    /**
     * @return the DatastoreProvider Morphia should use
     * @deprecated unused
     */
    @Deprecated
    public DatastoreProvider getDatastoreProvider() {
        return null;
    }

    /**
     * Sets the DatastoreProvider Morphia should use
     *
     * @param unused the DatastoreProvider to use
     * @deprecated unused
     */
    @Deprecated
    public void setDatastoreProvider(final DatastoreProvider unused) {
        LOG.warn("DatastoreProviders are no longer needed or used.");
    }

    /**
     * @return the mapper to use for top level entities
     */
    public CustomMapper getDefaultMapper() {
        return defaultMapper;
    }

    /**
     * Sets the mapper to use for top level entities
     *
     * @param pDefaultMapper the mapper to use
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setDefaultMapper(final CustomMapper pDefaultMapper) {
        defaultMapper = pDefaultMapper;
    }

    /**
     * @return the mapper to use for embedded entities
     */
    public CustomMapper getEmbeddedMapper() {
        return embeddedMapper;
    }

    /**
     * Sets the mapper to use for embedded entities
     *
     * @param pEmbeddedMapper the mapper to use
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setEmbeddedMapper(final CustomMapper pEmbeddedMapper) {
        embeddedMapper = pEmbeddedMapper;
    }

    /**
     * @return the factory to use when creating new instances
     */
    public ObjectFactory getObjectFactory() {
        if (objectFactory == null) {
            objectFactory = new DefaultCreator(this);
        }
        return objectFactory;
    }

    /**
     * Sets the ObjectFactory to use when instantiating entity classes.  The default factory is a simple reflection based factory but this
     * could be used, e.g., to provide a Guice-based factory such as what morphia-guice provides.
     *
     * @param objectFactory the factory to use
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setObjectFactory(final ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    /**
     * @return the mapper to use for references
     * @see Reference
     */
    public CustomMapper getReferenceMapper() {
        return referenceMapper;
    }

    /**
     * Sets the mapper to use for references
     *
     * @param pReferenceMapper the mapper to use
     * @see Reference
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setReferenceMapper(final CustomMapper pReferenceMapper) {
        referenceMapper = pReferenceMapper;
    }

    /**
     * @return the mapper to use when processing values
     */
    public CustomMapper getValueMapper() {
        return valueMapper;
    }

    /**
     * Sets the mapper to use when processing values
     *
     * @param pValueMapper the mapper to use
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setValueMapper(final CustomMapper pValueMapper) {
        valueMapper = pValueMapper;
    }

    /**
     * @return true if Morphia should ignore transient fields
     * @deprecated this is actually the default and proper behavior.  this setting is redundant
     */
    @Deprecated
    public boolean isActLikeSerializer() {
        return true;
    }

    /**
     * Instructs Morphia to follow JDK serialization semantics and ignore values marked up with the transient keyword
     *
     * @param ignored true if Morphia should ignore transient fields
     * @deprecated this is actually the default and proper behavior.  this setting is redundant
     */
    @Deprecated
    public void setActLikeSerializer(final boolean ignored) {
    }

    /**
     * @return true if Morphia should cache entities on lookups
     */
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    /**
     * @param enabled true if Morphia should cache entities on lookups
     * @return this
     */
    public MapperOptions setCachingEnabled(final boolean enabled) {
        cachingEnabled = enabled;
        return this;
    }

    /**
     * @return true if Morphia should cache name to Class lookups
     */
    public boolean isCacheClassLookups() {
        return cacheClassLookups;
    }

    /**
     * Sets whether Morphia should cache name to Class lookups
     *
     * @param cacheClassLookups true if the lookup results should be cached
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setCacheClassLookups(final boolean cacheClassLookups) {
        this.cacheClassLookups = cacheClassLookups;
    }

    /**
     * @return true if Morphia should ignore final fields
     */
    public boolean isIgnoreFinals() {
        return ignoreFinals;
    }

    /**
     * Controls if final fields are stored.
     *
     * @param ignoreFinals true if Morphia should ignore final fields
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setIgnoreFinals(final boolean ignoreFinals) {
        this.ignoreFinals = ignoreFinals;
    }

    /**
     * @return true if Morphia should store empty values for lists/maps/sets/arrays
     */
    public boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * Controls if Morphia should store empty values for lists/maps/sets/arrays
     *
     * @param storeEmpties true if Morphia should store empty values for lists/maps/sets/arrays
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setStoreEmpties(final boolean storeEmpties) {
        this.storeEmpties = storeEmpties;
    }

    /**
     * @return true if Morphia should store null values
     */
    public boolean isStoreNulls() {
        return storeNulls;
    }

    /**
     * Controls if null are stored.
     *
     * @param storeNulls true if Morphia should store null values
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setStoreNulls(final boolean storeNulls) {
        this.storeNulls = storeNulls;
    }

    /**
     * @return true if Morphia should use lower case values when calculating collection names
     */
    public boolean isUseLowerCaseCollectionNames() {
        return useLowerCaseCollectionNames;
    }

    /**
     * Controls if default entity collection name should be lowercase.
     *
     * @param useLowerCaseCollectionNames true if Morphia should use lower case values when calculating collection names
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setUseLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
        this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
    }

    /**
     * @return true if Morphia should skip scanning @{@link dev.morphia.annotations.Embedded} fields for index definitions.
     * @since 1.5
     */
    public boolean isDisableEmbeddedIndexes() {
        return disableEmbeddedIndexes;
    }

    /**
     * @return true if Morphia should map classes from the sub-packages as well
     */
    public boolean isMapSubPackages() {
        return mapSubPackages;
    }

    /**
     * Controls if classes from sub-packages should be mapped.
     *
     * @param mapSubPackages true if Morphia should map classes from the sub-packages as well
     * @see Builder
     * @deprecated use the Builder instead
     */
    @Deprecated
    public void setMapSubPackages(final boolean mapSubPackages) {
        this.mapSubPackages = mapSubPackages;
    }

    /**
     * @return the discriminator field name
     */
    public String getDiscriminatorField() {
        return Mapper.CLASS_NAME_FIELDNAME;
    }

    /**
     * Disables indexing of embedded types
     *
     * @param disableEmbeddedIndexes if true, @Embedded fields will not be scanned for indexing
     */
    public void setDisableEmbeddedIndexes(final boolean disableEmbeddedIndexes) {
        this.disableEmbeddedIndexes = disableEmbeddedIndexes;
    }

    /**
     * @return the format to use for Java 8 date/time storage
     */
    public DateStorage getDateStorage() {
        return dateStorage;
    }

    /**
     * This is used to determine how Java 8 dates and times are stored in the database.
     *
     * @param dateStorage the storage scheme to use for dates
     * @deprecated This will be removed in 2.0.  It is intended to bridge the gap when correcting the storage of data/time values in the
     * database.  {@link DateStorage#UTC} should be used and will be the default in 2.0.  In 1.5 it is {@link DateStorage#SYSTEM_DEFAULT}
     * for backwards compatibility.
     */
    @Deprecated
    public void setDateStorage(final DateStorage dateStorage) {
        this.dateStorage = dateStorage;
    }

    /**
     * @return a builder to set mapping options
     * @deprecated continued use of this method will result in different options being applied in 2.0.  To maintain the current settings
     * please use {@link #legacy()} instead.
     *
     * @see #legacy()
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder legacy() {
        return new Builder();
    }

    /**
     * @param original an existing set of options to use as a starting point
     * @return a builder to set mapping options
     */
    public static Builder builder(final MapperOptions original) {
        Builder builder = new Builder();
        builder.ignoreFinals = original.isIgnoreFinals();
        builder.storeNulls = original.isStoreNulls();
        builder.storeEmpties = original.isStoreEmpties();
        builder.useLowerCaseCollectionNames = original.isUseLowerCaseCollectionNames();
        builder.cacheClassLookups = original.isCacheClassLookups();
        builder.mapSubPackages = original.isMapSubPackages();
        builder.objectFactory = original.getObjectFactory();
        builder.cacheFactory = original.getCacheFactory();
        builder.embeddedMapper = original.getEmbeddedMapper();
        builder.defaultMapper = original.getDefaultMapper();
        builder.referenceMapper = original.getReferenceMapper();
        builder.valueMapper = original.getValueMapper();
        builder.disableEmbeddedIndexes = original.isDisableEmbeddedIndexes();
        builder.classLoader = original.getClassLoader();
        return builder;
    }

    /**
     * Returns the classloader used, in theory, when loading the entity types.
     *
     * @return the classloader
     *
     * @morphia.internal
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    /**
     * A builder class for setting mapping options
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        private boolean ignoreFinals;
        private boolean storeNulls;
        private boolean storeEmpties;
        private boolean useLowerCaseCollectionNames;
        private boolean cacheClassLookups;
        private boolean mapSubPackages;
        private boolean disableEmbeddedIndexes;
        private boolean cachingEnabled = true;
        private DateStorage dateStorage = DateStorage.SYSTEM_DEFAULT;
        private ObjectFactory objectFactory;
        private ClassLoader classLoader;
        private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
        private CustomMapper embeddedMapper = new EmbeddedMapper();
        private CustomMapper defaultMapper = embeddedMapper;
        private CustomMapper referenceMapper = new ReferenceMapper();
        private CustomMapper valueMapper = new ValueMapper();

        private Builder() {
        }

        /**
         * this method does nothing
         *
         * @param actLikeSerializer unused
         * @return this
         * @deprecated
         */
        @Deprecated
        public Builder actLikeSerializer(final boolean actLikeSerializer) {
            return this;
        }

        /**
         * @param ignoreFinals if true final fields are ignored
         * @return this
         */
        public Builder ignoreFinals(final boolean ignoreFinals) {
            this.ignoreFinals = ignoreFinals;
            return this;
        }

        /**
         * @param storeNulls if true null values are stored in the database
         * @return this
         */
        public Builder storeNulls(final boolean storeNulls) {
            this.storeNulls = storeNulls;
            return this;
        }

        /**
         * @param storeEmpties if true empty maps and collection types are stored in the database
         * @return this
         */
        public Builder storeEmpties(final boolean storeEmpties) {
            this.storeEmpties = storeEmpties;
            return this;
        }

        /**
         * @param useLowerCaseCollectionNames if true, generated collections names are lower cased
         * @return this
         */
        public Builder useLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
            this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
            return this;
        }

        /**
         * @param enable if true entities are cached during query result fetching
         * @return this
         */
        public Builder enableCaching(final boolean enable) {
            this.cachingEnabled = enable;
            return this;
        }

        /**
         * @param cacheClassLookups if true class lookups are cached
         * @return this
         */
        public Builder cacheClassLookups(final boolean cacheClassLookups) {
            this.cacheClassLookups = cacheClassLookups;
            return this;
        }

        /**
         * @param mapSubPackages if true subpackages are mapped when given a particular package
         * @return this
         */
        public Builder mapSubPackages(final boolean mapSubPackages) {
            this.mapSubPackages = mapSubPackages;
            return this;
        }

        /**
         * @param disableEmbeddedIndexes if true scanning @Embedded fields for indexing is disabled
         * @return this
         */
        public Builder disableEmbeddedIndexes(final boolean disableEmbeddedIndexes) {
            this.disableEmbeddedIndexes = disableEmbeddedIndexes;
            return this;
        }

        /**
         * @param objectFactory the object factory to use when creating instances
         * @return this
         */
        public Builder objectFactory(final ObjectFactory objectFactory) {
            this.objectFactory = objectFactory;
            return this;
        }

        /**
         * @param cacheFactory the cache factory to use
         * @return this
         */
        public Builder cacheFactory(final EntityCacheFactory cacheFactory) {
            this.cacheFactory = cacheFactory;
            return this;
        }

        /**
         * @param embeddedMapper the mapper to use for embedded entities
         * @return this
         */
        public Builder embeddedMapper(final CustomMapper embeddedMapper) {
            this.embeddedMapper = embeddedMapper;
            return this;
        }

        /**
         * @param defaultMapper the default mapper to use
         * @return this
         */
        public Builder defaultMapper(final CustomMapper defaultMapper) {
            this.defaultMapper = defaultMapper;
            return this;
        }

        /**
         * @param referenceMapper the mapper to use for references
         * @return this
         */
        public Builder referenceMapper(final CustomMapper referenceMapper) {
            this.referenceMapper = referenceMapper;
            return this;
        }

        /**
         * @param valueMapper the mapper use for values
         * @return this
         */
        public Builder valueMapper(final CustomMapper valueMapper) {
            this.valueMapper = valueMapper;
            return this;
        }

        /**
         * @param dateStorage the storage format to use for dates
         * @return this
         * @deprecated This will be removed in 2.0.  It is intended to bridge the gap when correcting the storage of data/time values in the
         * database.  {@link DateStorage#UTC} should be used and will be the default in 2.0.  In 1.5 it is
         * {@link DateStorage#SYSTEM_DEFAULT} for backwards compatibility.
         */
        @Deprecated
        public Builder dateForm(final DateStorage dateStorage) {
            this.dateStorage = dateStorage;
            return this;
        }

        /**
         * @param datastoreProvider the provider to use
         * @return this
         */
        public Builder datastoreProvider(final DatastoreProvider datastoreProvider) {
            return this;
        }

        /**
         * @param classLoader the ClassLoader to use
         * @return this
         */
        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * @return the new options instance
         */
        public MapperOptions build() {
            return new MapperOptions(this);
        }
    }
}
