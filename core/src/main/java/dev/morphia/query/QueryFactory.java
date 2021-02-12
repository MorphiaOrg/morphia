package dev.morphia.query;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A factory for {@link Query}ies.
 */
public interface QueryFactory {

    default <T> Query<T> createQuery(Datastore datastore, Class<T> type) {
        return createQuery(datastore, type, null);
    }

    /**
     * Creates and returns a {@link Query} for the given arguments. Default implementations of this method will simply delegate to {@link
     * #createQuery(Datastore, Class)} with the last argument being {@code null}.
     *
     * @param datastore  the Datastore to use
     * @param collection the actual collection to query.  This overrides any mapped on collection on type.
     * @param type       the type of the result
     * @param <T>        the type of the result
     * @return the query
     * @see #createQuery(Datastore, Class)
     */
    <T> Query<T> createQuery(Datastore datastore, String collection, Class<T> type);

    /**
     * Creates and returns a {@link Query} for the given arguments. The last argument is optional and may be {@code null}.
     *
     * @param datastore the Datastore to use
     * @param type      the type of the result
     * @param query     the Document containing the query structure
     * @param <T>       the type of the result
     * @return the query
     */
    <T> Query<T> createQuery(Datastore datastore, Class<T> type, @Nullable Document query);

    /**
     * Creates an unvalidated {@link Query} typically for use in aggregation pipelines.
     *
     * @param datastore the Datastore to use
     * @param <T>       the type of the result
     * @return the query
     * @deprecated this method is no longer used
     */
    @Deprecated(forRemoval = true)
    default <T> Query<T> createQuery(Datastore datastore) {
        throw new UnsupportedOperationException();
    }
}
