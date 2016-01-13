package org.mongodb.morphia.mapping;


import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.cache.DefaultEntityCacheFactory;
import org.mongodb.morphia.mapping.cache.EntityCacheFactory;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;


/**
 * Options to control mapping behavior.
 *
 * @author Scott Hernandez
 */
@SuppressWarnings("deprecation")
public class MapperOptions {
    private static final Logger LOG = MorphiaLoggerFactory.get(MapperOptions.class);
    private boolean actLikeSerializer;
    private boolean ignoreFinals; //ignore final fields.
    private boolean storeNulls;
    private boolean storeEmpties;
    private boolean useLowerCaseCollectionNames;
    private boolean cacheClassLookups = false;
    private boolean mapSubPackages = false;
    private ObjectFactory objectFactory = new DefaultCreator(this);
    private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
    private CustomMapper embeddedMapper = new EmbeddedMapper();
    private CustomMapper defaultMapper = embeddedMapper;
    private CustomMapper referenceMapper = new ReferenceMapper();
    private CustomMapper valueMapper = new ValueMapper();
    private DatastoreProvider datastoreProvider = null;

    /**
     * Creates a default options instance.
     */
    public MapperOptions() {
    }

    /**
     * Copy Constructor
     *
     * @param options the MapperOptions to copy
     */
    public MapperOptions(final MapperOptions options) {
        setActLikeSerializer(options.isActLikeSerializer());
        setIgnoreFinals(options.isIgnoreFinals());
        setStoreNulls(options.isStoreNulls());
        setStoreEmpties(options.isStoreEmpties());
        setUseLowerCaseCollectionNames(options.isUseLowerCaseCollectionNames());
        setCacheClassLookups(options.isCacheClassLookups());
        setObjectFactory(options.getObjectFactory());
        setCacheFactory(options.getCacheFactory());
        setEmbeddedMapper(options.getEmbeddedMapper());
        setDefaultMapper(options.getDefaultMapper());
        setReferenceMapper(options.getReferenceMapper());
        setValueMapper(options.getValueMapper());
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
     */
    public void setCacheFactory(final EntityCacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    /**
     * @return the DatastoreProvider Morphia should use
     * @deprecated unused
     */
    public DatastoreProvider getDatastoreProvider() {
        return datastoreProvider;
    }

    /**
     * Sets the DatastoreProvider Morphia should use
     *
     * @param datastoreProvider the DatastoreProvider to use
     * @deprecated unused
     */
    public void setDatastoreProvider(final DatastoreProvider datastoreProvider) {
        LOG.warning("DatastoreProviders are no longer needed or used.");
        this.datastoreProvider = datastoreProvider;
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
     */
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
     */
    public void setEmbeddedMapper(final CustomMapper pEmbeddedMapper) {
        embeddedMapper = pEmbeddedMapper;
    }

    /**
     * @return the factory to use when creating new instances
     */
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    /**
     * Sets the ObjectFactory to use when instantiating entity classes.  The default factory is a simple reflection based factory but this
     * could be used, e.g., to provide a Guice-based factory such as what morphia-guice provides.
     *
     * @param objectFactory the factory to use
     */
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
     */
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
     */
    public void setValueMapper(final CustomMapper pValueMapper) {
        valueMapper = pValueMapper;
    }

    /**
     * @return true if Morphia should ignore transient fields
     */
    public boolean isActLikeSerializer() {
        return actLikeSerializer;
    }

    /**
     * Instructs Morphia to follow JDK serialization semantics and ignore values marked up with the transient keyword
     *
     * @param actLikeSerializer true if Morphia should ignore transient fields
     */
    public void setActLikeSerializer(final boolean actLikeSerializer) {
        this.actLikeSerializer = actLikeSerializer;
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
     */
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
     */
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
     */
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
     */
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
     */
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
     */
    public void setMapSubPackages(final boolean mapSubPackages) {
        this.mapSubPackages = mapSubPackages;
    }
}
