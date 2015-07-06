package org.mongodb.morphia.mapping.lazy;


import org.mongodb.morphia.Datastore;


/**
 * for use with DatastoreProvider.Default
 */
public final class DatastoreHolder {
    private static final DatastoreHolder INSTANCE = new DatastoreHolder();
    private Datastore ds;

    private DatastoreHolder() {
    }

    /**
     * @return the 'singleton' DatastoreHolder
     */
    public static DatastoreHolder getInstance() {
        return INSTANCE;
    }

    /**
     * @return the Datastore being held
     */
    public Datastore get() {
        return ds;
    }

    /**
     * Sets the Datastore
     *
     * @param store the Datastore
     */
    public void set(final Datastore store) {
        ds = store;
    }
}
