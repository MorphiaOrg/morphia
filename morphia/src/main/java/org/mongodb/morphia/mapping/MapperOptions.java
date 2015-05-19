package org.mongodb.morphia.mapping;


import org.mongodb.morphia.ObjectFactory;


/**
 * Options to control mapping behavior.
 *
 * @author Scott Hernandez
 */
public class MapperOptions {
    private boolean actLikeSerializer;
    private boolean ignoreFinals; //ignore final fields.
    private boolean storeNulls;
    private boolean storeEmpties;
    private boolean useLowerCaseCollectionNames;

    private ObjectFactory objectFactory = new DefaultCreator();
    
    private CustomMapper embeddedMapper = new EmbeddedMapper();

    private CustomMapper defaultMapper = embeddedMapper;
    
    private CustomMapper referenceMapper = new ReferenceMapper();
    
    private CustomMapper valueMapper = new ValueMapper();

    public boolean isActLikeSerializer() {
        return actLikeSerializer;
    }

    /**
     * Treat java transient fields as if they have {@code @Transient} on them
     */
    public void setActLikeSerializer(final boolean actLikeSerializer) {
        this.actLikeSerializer = actLikeSerializer;
    }

    public CustomMapper getDefaultMapper() {
        return defaultMapper;
    }

    public void setDefaultMapper(final CustomMapper pDefaultMapper) {
        defaultMapper = pDefaultMapper;
    }

    public CustomMapper getEmbeddedMapper() {
        return embeddedMapper;
    }

    public void setEmbeddedMapper(final CustomMapper pEmbeddedMapper) {
        embeddedMapper = pEmbeddedMapper;
    }

    public boolean isIgnoreFinals() {
        return ignoreFinals;
    }

    /**
     * Controls if final fields are stored.
     */
    public void setIgnoreFinals(final boolean ignoreFinals) {
        this.ignoreFinals = ignoreFinals;
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    /**
     * Sets the ObjectFactory to use when instantiating entity classes.  The default factory is a simple reflection based factory but 
     * this could be used, e.g., to provide a Guice-based factory such as what morphia-guice provides.
     */
    public void setObjectFactory(final ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    public CustomMapper getReferenceMapper() {
        return referenceMapper;
    }

    public void setReferenceMapper(final CustomMapper pReferenceMapper) {
        referenceMapper = pReferenceMapper;
    }

    public boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * Controls if empty collection/arrays are stored.
     */
    public void setStoreEmpties(final boolean storeEmpties) {
        this.storeEmpties = storeEmpties;
    }

    public boolean isStoreNulls() {
        return storeNulls;
    }

    /**
     * Controls if null are stored.
     */
    public void setStoreNulls(final boolean storeNulls) {
        this.storeNulls = storeNulls;
    }

    public CustomMapper getValueMapper() {
        return valueMapper;
    }

    public void setValueMapper(final CustomMapper pValueMapper) {
        valueMapper = pValueMapper;
    }

    public boolean isUseLowerCaseCollectionNames() {
        return useLowerCaseCollectionNames;
    }

    /**
     * Controls if default entity collection name should be lowercase.
     */
    public void setUseLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
        this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
    }
}
