package dev.morphia.mapping.lazy;


import com.thoughtworks.proxy.factory.CglibProxyFactory;
import com.thoughtworks.proxy.toys.delegate.DelegationMode;
import com.thoughtworks.proxy.toys.dispatch.Dispatching;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import dev.morphia.mapping.lazy.proxy.CollectionObjectReference;
import dev.morphia.mapping.lazy.proxy.EntityObjectReference;
import dev.morphia.mapping.lazy.proxy.MapObjectReference;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;


/**
 * i have to admit, there are plenty of open questions for me on that Key-business...
 *
 * @author uwe schaefer
 */
@SuppressWarnings("unchecked")
public class CGLibLazyProxyFactory implements LazyProxyFactory {
    private final CglibProxyFactory factory = new CglibProxyFactory();

    @Override
    public <T extends Collection> T createListProxy(final Datastore datastore, final T listToProxy, final Class referenceObjClass,
                                                    final boolean ignoreMissing) {
        final Class<? extends Collection> targetClass = listToProxy.getClass();
        final CollectionObjectReference objectReference = new CollectionObjectReference(listToProxy, referenceObjClass, ignoreMissing,
                                                                                        datastore);

        final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[]{targetClass, Serializable.class}, factory, objectReference,
                                                                  DelegationMode.SIGNATURE).proxy();

        return (T) Dispatching.proxy(targetClass, new Class[]{ProxiedEntityReferenceList.class, targetClass, Serializable.class})
                              .with(objectReference, backend)
                              .build(factory);

    }

    @Override
    public <T extends Map> T createMapProxy(final Datastore datastore, final T mapToProxy, final Class referenceObjClass,
                                            final boolean ignoreMissing) {
        final Class<? extends Map> targetClass = mapToProxy.getClass();
        final MapObjectReference objectReference = new MapObjectReference(datastore, mapToProxy, referenceObjClass, ignoreMissing);

        final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[]{targetClass, Serializable.class}, factory, objectReference,
                                                                  DelegationMode.SIGNATURE).proxy();

        return (T) Dispatching.proxy(targetClass, new Class[]{ProxiedEntityReferenceMap.class, targetClass, Serializable.class})
                              .with(objectReference, backend)
                              .build(factory);

    }

    @Override
    public <T> T createProxy(final Datastore datastore, final Class<T> targetClass, final Key<T> key, final boolean ignoreMissing) {

        final EntityObjectReference objectReference = new EntityObjectReference(datastore, targetClass, key, ignoreMissing);

        final T backend = (T) new NonFinalizingHotSwappingInvoker(new Class[]{targetClass, Serializable.class}, factory, objectReference,
                                                                  DelegationMode.SIGNATURE).proxy();

        return (T) Dispatching.proxy(targetClass, new Class[]{ProxiedEntityReference.class, Serializable.class})
                              .with(objectReference, backend)
                              .build(factory);

    }
}
