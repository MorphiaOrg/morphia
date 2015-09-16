package org.mongodb.morphia.mapping.lazy;


import org.mongodb.morphia.Datastore;
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
     * @param <T>               the type of the entities
     * @param datastore         the Datastore to use when fetching this reference
     * @param listToProxy       the list to proxy
     * @param referenceObjClass the type of the referenced objects
     * @param ignoreMissing     ignore references that don't exist in the database
     * @return the proxy
     */
    <T extends Collection> T createListProxy(final Datastore datastore, T listToProxy, Class referenceObjClass, boolean ignoreMissing);

    /**
     * Creates a proxy for a Map.
     *
     * @param <T>               the type of the entities
     * @param datastore         the Datastore to use when fetching this reference
     * @param mapToProxy        the map to proxy
     * @param referenceObjClass the type of the referenced objects
     * @param ignoreMissing     ignore references that don't exist in the database
     * @return the proxy
     */
    <T extends Map> T createMapProxy(final Datastore datastore, final T mapToProxy, final Class referenceObjClass,
                                     final boolean ignoreMissing);

    /**
     * Creates a proxy for a Class.
     *
     * @param <T>               the type of the entity
     * @param datastore         the Datastore to use when fetching this reference
     * @param targetClass       the referenced object's Class
     * @param key               the Key of the reference
     * @return the proxy
     */
    <T> T createProxy(final Datastore datastore, Class<T> targetClass, final Key<T> key);

}
