package xyz.morphia.mapping;


import xyz.morphia.ObjectFactory;
import xyz.morphia.annotations.Reference;
import xyz.morphia.logging.Logger;
import xyz.morphia.logging.MorphiaLoggerFactory;
import xyz.morphia.mapping.cache.DefaultEntityCacheFactory;
import xyz.morphia.mapping.cache.EntityCacheFactory;
import xyz.morphia.mapping.lazy.DatastoreProvider;

/**
 * Options to control mapping behavior.
 */
@SuppressWarnings("deprecation")
public class MapperOptions {
    private static final Logger LOG = MorphiaLoggerFactory.get(MapperOptions.class);
    private boolean ignoreFinals; //ignore final fields.
    private boolean storeNulls;
    private boolean storeEmpties;
    private boolean useLowerCaseCollectionNames;
    private boolean cacheClassLookups;
    private boolean mapSubPackages;
    private ObjectFactory objectFactory;
    private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
    private CustomMapper embeddedMapper = new EmbeddedMapper();
    private CustomMapper defaultMapper = embeddedMapper;
    private CustomMapper referenceMapper = new ReferenceMapper();
    private CustomMapper valueMapper = new ValueMapper();

    /**
     * Creates a default options instance.
     * @deprecated use the Builder instead
     * @see #builder()
     * @see Builder
     */
    @Deprecated
    public MapperOptions() {
    }

    /**
     * Copy Constructor
     *
     * @param options the MapperOptions to copy
     * @deprecated use the Builder instead
     * @see #builder(MapperOptions)
     * @see Builder
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
    }

    private MapperOptions(final Builder builder) {
        ignoreFinals = builder.ignoreFinals;
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @param ignored the DatastoreProvider to use
     * @deprecated unused
     */
    @Deprecated
    public void setDatastoreProvider(final DatastoreProvider ignored) {
        LOG.warning("DatastoreProviders are no longer needed or used.");
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
     */
    @Deprecated
    public void setEmbeddedMapper(final CustomMapper pEmbeddedMapper) {
        embeddedMapper = pEmbeddedMapper;
    }

    /**
     * @return the factory to use when creating new instances
     */
    public ObjectFactory getObjectFactory() {
        if(objectFactory == null) {
            objectFactory = new DefaultCreator(this);
        }
        return objectFactory;
    }

    /**
     * Sets the ObjectFactory to use when instantiating entity classes.  The default factory is a simple reflection based factory but this
     * could be used, e.g., to provide a Guice-based factory such as what morphia-guice provides.
     *
     * @param objectFactory the factory to use
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @return true if Morphia should cache name -> Class lookups
     */
    public boolean isCacheClassLookups() {
        return cacheClassLookups;
    }

    /**
     * Sets whether Morphia should cache name -> Class lookups
     *
     * @param cacheClassLookups true if the lookup results should be cached
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
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
     * @deprecated use the Builder instead
     * @see Builder
     */
    @Deprecated
    public void setUseLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
        this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
    }

    /**
     * @return true if Morphia should map classes from the sub-packages as well
     */
    public boolean isMapSubPackages() {
        return mapSubPackages;
    }

    /**
     * Controls if classes from sub-packages should be mapped.
     * @param mapSubPackages true if Morphia should map classes from the sub-packages as well
     * @deprecated use the Builder instead
     * @see Builder
     */
    @Deprecated
    public void setMapSubPackages(final boolean mapSubPackages) {
        this.mapSubPackages = mapSubPackages;
    }

    public String getDiscriminatorField() {
        return Mapper.CLASS_NAME_FIELDNAME;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(final MapperOptions copy) {
        Builder builder = new Builder();
        builder.ignoreFinals = copy.isIgnoreFinals();
        builder.storeNulls = copy.isStoreNulls();
        builder.storeEmpties = copy.isStoreEmpties();
        builder.useLowerCaseCollectionNames = copy.isUseLowerCaseCollectionNames();
        builder.cacheClassLookups = copy.isCacheClassLookups();
        builder.mapSubPackages = copy.isMapSubPackages();
        builder.objectFactory = copy.getObjectFactory();
        builder.cacheFactory = copy.getCacheFactory();
        builder.embeddedMapper = copy.getEmbeddedMapper();
        builder.defaultMapper = copy.getDefaultMapper();
        builder.referenceMapper = copy.getReferenceMapper();
        builder.valueMapper = copy.getValueMapper();
        return builder;
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private boolean ignoreFinals;
        private boolean storeNulls;
        private boolean storeEmpties;
        private boolean useLowerCaseCollectionNames;
        private boolean cacheClassLookups;
        private boolean mapSubPackages;
        private ObjectFactory objectFactory;
        private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
        private CustomMapper embeddedMapper = new EmbeddedMapper();
        private CustomMapper defaultMapper = embeddedMapper;
        private CustomMapper referenceMapper = new ReferenceMapper();
        private CustomMapper valueMapper = new ValueMapper();

        private Builder() {
        }

        public Builder actLikeSerializer(final boolean actLikeSerializer) {
            return this;
        }

        public Builder ignoreFinals(final boolean ignoreFinals) {
            this.ignoreFinals = ignoreFinals;
            return this;
        }

        public Builder storeNulls(final boolean storeNulls) {
            this.storeNulls = storeNulls;
            return this;
        }

        public Builder storeEmpties(final boolean storeEmpties) {
            this.storeEmpties = storeEmpties;
            return this;
        }

        public Builder useLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
            this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
            return this;
        }

        public Builder cacheClassLookups(final boolean cacheClassLookups) {
            this.cacheClassLookups = cacheClassLookups;
            return this;
        }

        public Builder mapSubPackages(final boolean mapSubPackages) {
            this.mapSubPackages = mapSubPackages;
            return this;
        }

        public Builder objectFactory(final ObjectFactory objectFactory) {
            this.objectFactory = objectFactory;
            return this;
        }

        public Builder cacheFactory(final EntityCacheFactory cacheFactory) {
            this.cacheFactory = cacheFactory;
            return this;
        }

        public Builder embeddedMapper(final CustomMapper embeddedMapper) {
            this.embeddedMapper = embeddedMapper;
            return this;
        }

        public Builder defaultMapper(final CustomMapper defaultMapper) {
            this.defaultMapper = defaultMapper;
            return this;
        }

        public Builder referenceMapper(final CustomMapper referenceMapper) {
            this.referenceMapper = referenceMapper;
            return this;
        }

        public Builder valueMapper(final CustomMapper valueMapper) {
            this.valueMapper = valueMapper;
            return this;
        }

        public Builder datastoreProvider(final DatastoreProvider datastoreProvider) {
            return this;
        }

        public MapperOptions build() {
            return new MapperOptions(this);
        }
    }
}
