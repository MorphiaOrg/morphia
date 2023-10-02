package dev.morphia.query.internal;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Indicates a type that can accept a {@link MorphiaDatastore}
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public interface DatastoreAware {
    /**
     * @param datastore the datastore
     * @morphia.internal
     */
    void setDatastore(MorphiaDatastore datastore);
}
