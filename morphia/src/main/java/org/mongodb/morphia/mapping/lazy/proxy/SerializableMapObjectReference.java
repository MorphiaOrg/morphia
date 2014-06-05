package org.mongodb.morphia.mapping.lazy.proxy;


import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class SerializableMapObjectReference extends AbstractReference implements ProxiedEntityReferenceMap {

    private static final long serialVersionUID = 1L;
    private final HashMap<Object, Key<?>> keyMap;

    public SerializableMapObjectReference(final Map mapToProxy, final Class referenceObjClass, final boolean ignoreMissing,
                                          final DatastoreProvider p) {

        super(p, referenceObjClass, ignoreMissing);
        object = mapToProxy;
        keyMap = new LinkedHashMap<Object, Key<?>>();
    }

    //CHECKSTYLE:OFF
    public void __put(final Object key, final Key k) {
        keyMap.put(key, k);
    }
    //CHECKSTYLE:ON

    @Override
    @SuppressWarnings("unchecked")
    protected Object fetch() {
        final Map m = (Map) object;
        m.clear();
        // TODO us: change to getting them all at once and yell according to
        // ignoreMissing in order to a) increase performance and b) resolve
        // equals keys to the same instance
        // that should really be done in datastore.
        for (final Map.Entry<?, Key<?>> e : keyMap.entrySet()) {
            final Key<?> entityKey = e.getValue();
            m.put(e.getKey(), fetch(entityKey));
        }
        return m;
    }

    @Override
    protected void beforeWriteObject() {
        if (__isFetched()) {
            syncKeys();
            ((Map) object).clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void syncKeys() {
        final Datastore ds = p.get();

        keyMap.clear();
        final Map<Object, Object> map = (Map) object;
        for (final Map.Entry<Object, Object> e : map.entrySet()) {
            keyMap.put(e.getKey(), ds.getKey(e.getValue()));
        }
    }

    //CHECKSTYLE:OFF
    public Map<Object, Key<?>> __getReferenceMap() {
        return keyMap;
    }
    //CHECKSTYLE:ON

}
