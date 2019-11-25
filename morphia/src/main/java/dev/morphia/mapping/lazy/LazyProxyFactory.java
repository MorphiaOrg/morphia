package dev.morphia.mapping.lazy;


import dev.morphia.Datastore;
import dev.morphia.Key;

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
    <T extends Collection> T createListProxy(Datastore datastore, T listToProxy, Class referenceObjClass, boolean ignoreMissing);

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
    <T extends Map> T createMapProxy(Datastore datastore, T mapToProxy, Class referenceObjClass, boolean ignoreMissing);

    /**
     * Creates a proxy for a Class.
     *
     * @param <T>               the type of the entity
     * @param datastore         the Datastore to use when fetching this reference
     * @param targetClass       the referenced object's Class
     * @param key               the Key of the reference
     * @param ignoreMissing     ignore references that don't exist in the database
     * @return the proxy
     */
    <T> T createProxy(Datastore datastore, Class<T> targetClass, Key<T> key, boolean ignoreMissing);
}
