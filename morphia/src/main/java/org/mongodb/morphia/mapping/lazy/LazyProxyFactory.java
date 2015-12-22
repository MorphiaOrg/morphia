package org.mongodb.morphia.mapping.lazy;


import org.mongodb.morphia.Key;

import java.util.Collection;
import java.util.Map;


/**
 * @author uwe schaefer
 */
public interface LazyProxyFactory {
    /**
     * Creates a proxy for a List.
     *
     * @param listToProxy       the list to proxy
     * @param referenceObjClass the type of the referenced objects
     * @param ignoreMissing     ignore references that don't exist in the database
     * @param datastoreProvider the DatastoreProvider to use
     * @param <T>               the type of the entities
     * @return the proxy
     */
    <T extends Collection> T createListProxy(T listToProxy, Class referenceObjClass, boolean ignoreMissing,
                                             DatastoreProvider datastoreProvider);

    /**
     * Creates a proxy for a Map.
     *
     * @param mapToProxy        the map to proxy
     * @param referenceObjClass the type of the referenced objects
     * @param ignoreMissing     ignore references that don't exist in the database
     * @param datastoreProvider the DatastoreProvider to use
     * @param <T>               the type of the entities
     * @return the proxy
     */
    <T extends Map> T createMapProxy(final T mapToProxy, final Class referenceObjClass, final boolean ignoreMissing,
                                     final DatastoreProvider datastoreProvider);

    /**
     * Creates a proxy for a Class.
     *
     * @param targetClass       the referenced object's Class
     * @param key               the Key of the reference
     * @param datastoreProvider the DatastoreProvider to use
     * @param <T>               the type of the entity
     * @return the proxy
     */
    <T> T createProxy(Class<T> targetClass, final Key<T> key, final DatastoreProvider datastoreProvider);

}
