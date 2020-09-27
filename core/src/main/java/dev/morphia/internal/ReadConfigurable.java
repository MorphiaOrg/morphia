package dev.morphia.internal;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;

/**
 * @param <T> the type being defined
 * @morphia.internal
 * @since 2.0
 */
public interface ReadConfigurable<T> {
    /**
     * Gets the read concern
     *
     * @return the read concern
     */
    ReadConcern getReadConcern();

    /**
     * @return the read preference
     */
    ReadPreference getReadPreference();

    default <C> MongoCollection<C> prepare(MongoCollection<C> collection) {
        MongoCollection<C> updated = collection;
        if (getReadConcern() != null) {
            updated = updated.withReadConcern(getReadConcern());
        }
        if (getReadPreference() != null) {
            updated = updated.withReadPreference(getReadPreference());
        }

        return updated;
    }

    /**
     * Sets the read concern to apply
     *
     * @param readConcern the read concern
     * @return this
     */
    T readConcern(ReadConcern readConcern);

    /**
     * Sets the read preference to apply
     *
     * @param readPreference the read preference
     * @return this
     */
    T readPreference(ReadPreference readPreference);
}
