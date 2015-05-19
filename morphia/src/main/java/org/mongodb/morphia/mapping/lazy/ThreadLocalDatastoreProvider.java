package org.mongodb.morphia.mapping.lazy;

import org.mongodb.morphia.Datastore;

/**
 * Stores one active datastore per thread, based on the most recently registered datastore
 * 
 * @author Michael Houston
 */
public class ThreadLocalDatastoreProvider implements DatastoreProvider {

    private static final long serialVersionUID = 1L;

    private final transient ThreadLocal<Datastore> datastores = new ThreadLocal<Datastore>();

    @Override
    public Datastore get() {
        Datastore datastore = datastores.get();
        if (datastore == null) {
            throw new IllegalStateException(
                    "ThreadLocal does not carry a Datastore for this thread.");
        }
        return datastore;
    }

    @Override
    public void register(final Datastore ds) {
        datastores.set(ds);
    }

}
