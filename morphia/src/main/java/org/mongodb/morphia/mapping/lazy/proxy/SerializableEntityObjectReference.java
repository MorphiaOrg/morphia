package org.mongodb.morphia.mapping.lazy.proxy;


import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;

import static java.lang.String.format;


/**
 * A serializable object reference
 */
public class SerializableEntityObjectReference extends AbstractReference implements ProxiedEntityReference {
    private static final long serialVersionUID = 1L;
    private final Key key;

    /**
     * Creates a serializable object reference
     *
     * @param targetClass the Class of the referenced item
     * @param p           the DatastoreProvider to use
     * @param key         the Key value
     */
    public SerializableEntityObjectReference(final Class targetClass, final DatastoreProvider p, final Key key) {

        super(p, targetClass, false);
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
        final Object entity = p.get().getByKey(referenceObjClass, key);
        if (entity == null) {
            throw new LazyReferenceFetchingException(format("During the lifetime of the proxy, the Entity identified by '%s' "
                                                            + "disappeared from the Datastore.", key));
        }
        return entity;
    }
}
