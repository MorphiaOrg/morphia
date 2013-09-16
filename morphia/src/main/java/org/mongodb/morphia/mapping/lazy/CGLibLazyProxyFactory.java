package org.mongodb.morphia.mapping.lazy;


import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import org.mongodb.morphia.mapping.lazy.proxy.SerializableCollectionObjectReference;
import org.mongodb.morphia.mapping.lazy.proxy.SerializableEntityObjectReference;
import org.mongodb.morphia.mapping.lazy.proxy.SerializableMapObjectReference;
import com.thoughtworks.proxy.factory.CglibProxyFactory;
import com.thoughtworks.proxy.toys.delegate.DelegationMode;
import com.thoughtworks.proxy.toys.dispatch.Dispatching;


/**
 * i have to admit, there are plenty of open questions for me on that Key-business...
 *
 * @author uwe schaefer
 */
@SuppressWarnings("unchecked")
public class CGLibLazyProxyFactory implements LazyProxyFactory {
  private final CglibProxyFactory factory = new CglibProxyFactory();

  public <T> T createProxy(final Class<T> targetClass, final Key<T> key, final DatastoreProvider p) {

    final SerializableEntityObjectReference objectReference = new SerializableEntityObjectReference(targetClass, p, key);

    final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[] {targetClass, Serializable.class}, factory, objectReference,
      DelegationMode.SIGNATURE).proxy();

    return Dispatching.proxy(targetClass, new Class[] {ProxiedEntityReference.class, targetClass, Serializable.class}).with(objectReference,
      backend).build(factory);

  }

  public <T extends Collection> T createListProxy(final T listToProxy, final Class referenceObjClass, final boolean ignoreMissing,
    final DatastoreProvider p) {
    final Class<? extends Collection> targetClass = listToProxy.getClass();
    final SerializableCollectionObjectReference objectReference = new SerializableCollectionObjectReference(listToProxy, referenceObjClass,
      ignoreMissing, p);

    final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[] {targetClass, Serializable.class}, factory, objectReference,
      DelegationMode.SIGNATURE).proxy();

    return (T) Dispatching.proxy(targetClass, new Class[] {ProxiedEntityReferenceList.class, targetClass, Serializable.class}).with(
      objectReference, backend).build(factory);

  }

  public <T extends Map> T createMapProxy(final T mapToProxy, final Class referenceObjClass, final boolean ignoreMissing,
    final DatastoreProvider p) {
    final Class<? extends Map> targetClass = mapToProxy.getClass();
    final SerializableMapObjectReference objectReference = new SerializableMapObjectReference(mapToProxy, referenceObjClass, ignoreMissing,
      p);

    final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[] {targetClass, Serializable.class}, factory, objectReference,
      DelegationMode.SIGNATURE).proxy();

    return (T) Dispatching.proxy(targetClass, new Class[] {ProxiedEntityReferenceMap.class, targetClass, Serializable.class}).with(
      objectReference, backend).build(factory);

  }
}
