package dev.morphia.query.internal;

import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Indicates a type that can accept a {@link dev.morphia.DatastoreImpl}
 *
 * @morphia.internal
 */
@MorphiaInternal
public interface DatastoreAware {
    /**
     * @param datastore the datastore
     * @morphia.internal
     */
    void setDatastore(DatastoreImpl datastore);
}
