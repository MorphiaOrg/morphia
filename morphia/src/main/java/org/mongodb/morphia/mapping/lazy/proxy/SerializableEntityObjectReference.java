package org.mongodb.morphia.mapping.lazy.proxy;


import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;

import static java.lang.String.format;


public class SerializableEntityObjectReference extends AbstractReference implements ProxiedEntityReference {
    private static final long serialVersionUID = 1L;
    private final Key key;

    public SerializableEntityObjectReference(final Class targetClass, final DatastoreProvider p, final Key key) {

        super(p, targetClass, false);
        this.key = key;
    }

    //CHECKSTYLE:OFF
    public Key __getKey() {
        return key;
    }
    //CHECKSTYLE:ON

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

    @Override
    protected void beforeWriteObject() {
        object = null;
    }
}