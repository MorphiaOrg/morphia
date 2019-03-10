package dev.morphia.mapping.lazy.proxy;


import dev.morphia.Datastore;
import dev.morphia.Key;

import static java.lang.String.format;


/**
 * A serializable object reference
 */
public class EntityObjectReference extends AbstractReference implements ProxiedEntityReference {
    private static final long serialVersionUID = 1L;
    private final Key key;

    /**
     * Creates an object reference
     *
     * @param datastore     the Datastore to use when fetching this reference
     * @param targetClass   the Class of the referenced item
     * @param key           the Key value
     * @param ignoreMissing ignore references that don't exist in the database
     */
    public EntityObjectReference(final Datastore datastore, final Class targetClass, final Key key, final boolean ignoreMissing) {
        super(datastore, targetClass, ignoreMissing);
        this.key = key;
    }

    //CHECKSTYLE:OFF
    @Override
    public Key __getKey() {
        return key;
    }
    //CHECKSTYLE:ON

    @Override
    protected void beforeWriteObject() {
        object = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object fetch() {
        final Object entity = getDatastore().getByKey(referenceObjClass, key);
        if (entity == null && !ignoreMissing) {
            throw new LazyReferenceFetchingException(format("During the lifetime of the proxy, the Entity identified by '%s' "
                                                                + "disappeared from the Datastore.", key));
        }
        return entity;
    }
}
