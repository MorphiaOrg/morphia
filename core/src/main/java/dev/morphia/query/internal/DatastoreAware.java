package dev.morphia.query.internal;

import dev.morphia.DatastoreImpl;

/**
 * Indicates a type that can accept a {@link dev.morphia.DatastoreImpl}
 *
 * @morphia.internal
 */
public interface DatastoreAware {
    /**
     * @param datastore the datastore
     * @morphia.internal
     */
    void setDatastore(DatastoreImpl datastore);
}
