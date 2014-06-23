package org.mongodb.morphia.mapping.lazy.proxy;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class SerializableCollectionObjectReference<T> extends AbstractReference implements ProxiedEntityReferenceList {

    private static final long serialVersionUID = 1L;
    private final List<Key<?>> listOfKeys;

    public SerializableCollectionObjectReference(final Collection<T> type, final Class<T> referenceObjClass, final boolean ignoreMissing,
                                                 final DatastoreProvider p) {

        super(p, referenceObjClass, ignoreMissing);

        object = type;
        listOfKeys = new ArrayList<Key<?>>();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected synchronized Object fetch() {
        final Collection<T> c = (Collection<T>) object;
        c.clear();

        final int numberOfEntitiesExpected = listOfKeys.size();
        // does not retain order:
        // List<T> retrievedEntities = p.get().getByKeys(referenceObjClass,
        // (List) __getKeysAsList());

        // so we do it the lousy way: FIXME
        final List<T> retrievedEntities = new ArrayList<T>(listOfKeys.size());
        final Datastore ds = p.get();
        for (final Key<?> k : listOfKeys) {
            retrievedEntities.add((T) ds.getByKey(referenceObjClass, k));
        }

        if (!ignoreMissing && (numberOfEntitiesExpected != retrievedEntities.size())) {
            throw new LazyReferenceFetchingException(format("During the lifetime of a proxy of type '%s', some referenced Entities"
                                                            + " of type '%s' have disappeared from the Datastore.",
                                                            c.getClass().getSimpleName(), referenceObjClass.getSimpleName()));
        }

        c.addAll(retrievedEntities);
        return c;
    }

    //CHECKSTYLE:OFF
    public List<Key<?>> __getKeysAsList() {
        return Collections.unmodifiableList(listOfKeys);
    }

    public void __add(final Key key) {
        listOfKeys.add(key);
    }

    public void __addAll(final Collection<? extends Key<?>> keys) {
        listOfKeys.addAll(keys);
    }
    //CHECKSTYLE:ON

    @Override
    @SuppressWarnings("unchecked")
    protected void beforeWriteObject() {
        if (__isFetched()) {
            syncKeys();
            ((Collection<T>) object).clear();
        }
    }

    private void syncKeys() {
        final Datastore ds = p.get();

        listOfKeys.clear();
        for (final Object e : ((Collection) object)) {
            listOfKeys.add(ds.getKey(e));
        }
    }
}