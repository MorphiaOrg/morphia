package dev.morphia.internal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Specifies that an Options class accepts an alternate collection specification.
 *
 * @param <T> the Options type
 * @hidden
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
     * @return either the alternate collection, with the mapped collection's write concern, if one is defined or the collection passed in
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    default <T> MongoCollection<T> prepare(@Nullable MongoCollection<T> collection, MongoDatabase database) {
        String alternateName = collection();
        if (alternateName != null) {
            // the mapped collection carries the entity's write concern, which applies wherever the entity is written
            collection = database.getCollection(alternateName, collection.getDocumentClass())
                    .withWriteConcern(collection.getWriteConcern());
        }

        return collection;
    }

    /**
     * Returns the alternate collection to use for the operation. Might return null.
     *
     * @return the collection name or null
     * @since 2.3
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    String collection();
}
