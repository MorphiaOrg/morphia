package org.mongodb.morphia.mapping;


import org.mongodb.morphia.ObjectFactory;


/**
 * Options to control mapping behavior.
 *
 * @author Scott Hernandez
 */
public class MapperOptions {
    //CHECKSTYLE:OFF
    /**
     * <p>Treat java transient fields as if they have {@code @Transient} on them</p>
     *
     * @deprecated use the getter/setter instead
     */
    public boolean actLikeSerializer;
    /**
     * <p>Controls if null are stored. </p>
     *
     * @deprecated use the getter/setter instead
     */
    public boolean storeNulls;
    /**
     * <p>Controls if empty collection/arrays are stored. </p>
     *
     * @deprecated use the getter/setter instead
     */
    public boolean storeEmpties;
    /**
     * <p>Controls if final fields are stored. </p>
     *
     * @deprecated use the getter/setter instead
     */
    public boolean ignoreFinals; //ignore final fields.
    /**
     * @deprecated use the getter/setter instead
     */
    public final CustomMapper referenceMapper = new ReferenceMapper();
    /**
     * @deprecated use the getter/setter instead
     */
    public final CustomMapper embeddedMapper = new EmbeddedMapper();
    /**
     * @deprecated use the getter/setter instead
     */
    public final CustomMapper valueMapper = new ValueMapper();
    /**
     * @deprecated use the getter/setter instead
     */
    public final CustomMapper defaultMapper = embeddedMapper;

    public ObjectFactory objectFactory = new DefaultCreator();
    //CHECKSTYLE:ON

    public boolean isActLikeSerializer() {
        return actLikeSerializer;
    }

    public void setActLikeSerializer(final boolean actLikeSerializer) {
        this.actLikeSerializer = actLikeSerializer;
    }

    public CustomMapper getDefaultMapper() {
        return defaultMapper;
    }

    public CustomMapper getEmbeddedMapper() {
        return embeddedMapper;
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
}
