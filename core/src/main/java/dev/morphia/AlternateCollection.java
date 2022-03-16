package dev.morphia;

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
public interface AlternateCollection<T> {
    /**
     * Sets the alternate collection to use for the operation.
     *
     * @param collection the name of the collection to use
     * @return this
     * @since 2.3
     */
    T collection(@Nullable String collection);

    /**
     * Returns the alternate collection to use for the operation.  Might return null.
     *
     * @return the collection name or null
     * @since 2.3
     */
    @Nullable
    String collection();

}
