package org.mongodb.morphia.mapping.lazy;


import java.util.Collection;
import java.util.Map;

import org.mongodb.morphia.Key;


/**
 * @author uwe schaefer
 */
public interface LazyProxyFactory {
  <T> T createProxy(Class<T> targetClass, final Key<T> key, final DatastoreProvider p);

  <T extends Collection> T createListProxy(T listToProxy, Class referenceObjClass, boolean ignoreMissing, DatastoreProvider p);

  <T extends Map> T createMapProxy(final T mapToProxy, final Class referenceObjClass, final boolean ignoreMissing,
    final DatastoreProvider p);

}
