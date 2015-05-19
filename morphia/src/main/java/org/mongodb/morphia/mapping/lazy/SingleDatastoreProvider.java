package org.mongodb.morphia.mapping.lazy;

import org.mongodb.morphia.Datastore;

/**
 * This will only ever return the first datastore created by an associated
 * Mapper instance
 * 
 * @author Michael Houston
 */
public class SingleDatastoreProvider implements DatastoreProvider {

    private static final long serialVersionUID = 1L;

    private transient Datastore datastore;

    @Override
    public Datastore get() {
        if (datastore == null) {
            throw new IllegalStateException(
                    "SingleDatastoreProvider does not carry a Datastore.");
        }
        return datastore;
    }

    @Override
    public void register(final Datastore ds) {
        if (datastore == null) {
            datastore = ds;
        }
    }

}
