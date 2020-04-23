package dev.morphia.internal;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;

public interface WriteConfigurable<T> {
    /**
     * The write concern to use.  By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     * @deprecated use {@link #writeConcern()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default WriteConcern getWriteConcern() {
        return writeConcern();
    }

    /**
     * Applies the options to the collection
     *
     * @param collection the collection to prepare
     * @param <C>        the collection type
     * @return either the passed collection or the updated collection
     * @since 2.0
     */
    default <C> MongoCollection<C> prepare(MongoCollection<C> collection) {
        return writeConcern() == null
               ? collection
               : collection.withWriteConcern(writeConcern());
    }

    /**
     * Set the write concern to use.
     *
     * @param writeConcern the write concern
     * @return this
     */
    T writeConcern(WriteConcern writeConcern);

    /**
     * The write concern to use.  By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     */
    WriteConcern writeConcern();
}
