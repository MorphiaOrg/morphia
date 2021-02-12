package dev.morphia.query;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A default implementation of {@link QueryFactory}.
 */
public class DefaultQueryFactory implements QueryFactory {
    @Override
    public <T> Query<T> createQuery(Datastore datastore, String collection, Class<T> type) {
        return new MorphiaQuery<>(datastore, collection, type);
    }

    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type, @Nullable Document query) {
        return query != null
               ? new MorphiaQuery<>(datastore, type, query)
               : new MorphiaQuery<>(datastore, null, type);
    }
}
