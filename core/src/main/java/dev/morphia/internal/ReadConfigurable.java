package dev.morphia.internal;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @param <T> the type being defined
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public interface ReadConfigurable<T> extends CollectionConfiguration {
    /**
     * Gets the read concern
     *
     * @return the read concern
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    @Deprecated(forRemoval = true, since = "2.3")
    default ReadConcern getReadConcern() {
        return readConcern();
    }

    /**
     * @return the read preference
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    @Deprecated(forRemoval = true, since = "2.3")
    default ReadPreference getReadPreference() {
        return readPreference();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    default <C> MongoCollection<C> prepare(MongoCollection<C> collection) {
        MongoCollection<C> updated = collection;
        ReadConcern readConcern = readConcern();
        if (readConcern != null) {
            updated = updated.withReadConcern(readConcern);
        }
        ReadPreference readPreference = readPreference();
        if (readPreference != null) {
            updated = updated.withReadPreference(readPreference);
        }

        return updated;
    }

    /**
     * Gets the read concern
     *
     * @return the read concern
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    ReadConcern readConcern();

    /**
     * @return the read preference
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    ReadPreference readPreference();

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
