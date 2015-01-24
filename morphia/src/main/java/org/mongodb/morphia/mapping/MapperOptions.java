package org.mongodb.morphia.mapping;


import org.mongodb.morphia.ObjectFactory;


/**
 * Options to control mapping behavior.
 *
 * @author Scott Hernandez
 */
public class MapperOptions {
    /**
     * <p>Treat java transient fields as if they have {@code @Transient} on them</p>
     */
    private boolean actLikeSerializer;
    /**
     * <p>Controls if final fields are stored. </p>
     */
    private boolean ignoreFinals; //ignore final fields.
    /**
     * <p>Controls if null are stored. </p>
     */
    private boolean storeNulls;
    /**
     * <p>Controls if empty collection/arrays are stored. </p>
     */
    private boolean storeEmpties;
    /**
     * <p>Controls if default entity collection name should be lowercase.</p>
     */
    private boolean lowercaseDefault;

    private ObjectFactory objectFactory = new DefaultCreator();
    
    private CustomMapper embeddedMapper = new EmbeddedMapper();

    private CustomMapper defaultMapper = embeddedMapper;
    
    private CustomMapper referenceMapper = new ReferenceMapper();
    
    private CustomMapper valueMapper = new ValueMapper();

    public boolean isActLikeSerializer() {
        return actLikeSerializer;
    }

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

    public void setIgnoreFinals(final boolean ignoreFinals) {
        this.ignoreFinals = ignoreFinals;
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

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

    public void setStoreEmpties(final boolean storeEmpties) {
        this.storeEmpties = storeEmpties;
    }

    public boolean isStoreNulls() {
        return storeNulls;
    }

    public void setStoreNulls(final boolean storeNulls) {
        this.storeNulls = storeNulls;
    }

    public CustomMapper getValueMapper() {
        return valueMapper;
    }

    public void setValueMapper(final CustomMapper pValueMapper) {
        valueMapper = pValueMapper;
    }

    public boolean shouldLowercaseDefault() {
        return lowercaseDefault;
    }

    public void setLowercaseDefault(final boolean lowercaseDefault) {
        this.lowercaseDefault = lowercaseDefault;
    }
}
