package dev.morphia.internal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Specifies that an Options class accepts an alternate collection specification.
 *
 * @param <T> the Options type
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
public interface CollectionConfigurable<T> extends CollectionConfiguration {
    /**
     * Sets the alternate collection to use for the operation.
     *
     * @param collection the name of the collection to use
     * @return this
     * @since 2.3
     */
    T collection(@Nullable String collection);

    /**
     * Returns an alternate collection if one is defined on this type.
     *
     * @param collection the mapped collection
     * @param database   the database
     * @param <T>        the collection type
     * @return either the alternate collection if one is defined or the collection passed in
     */
    default <T> MongoCollection<T> prepare(@Nullable MongoCollection<T> collection, MongoDatabase database) {
        String alternateName = collection();
        if (alternateName != null) {
            collection = database.getCollection(alternateName, collection.getDocumentClass());
        }

        return collection;
    }

    /**
     * Returns the alternate collection to use for the operation. Might return null.
     *
     * @return the collection name or null
     * @since 2.3
     */
    @Nullable
    String collection();
}
