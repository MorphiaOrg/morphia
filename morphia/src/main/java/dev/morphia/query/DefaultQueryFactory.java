package dev.morphia.query;

import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A default implementation of {@link QueryFactory}.
 */
public class DefaultQueryFactory extends AbstractQueryFactory {
    @Override
    public <T> Query<T> createQuery(Datastore datastore, String collection, Class<T> type) {
        final MorphiaQuery<T> query = new MorphiaQuery<>(datastore, collection, type);

        return query;
    }

    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type, Document query) {
        final MorphiaQuery<T> item;

        if (query != null) {
            item = new MorphiaQuery<>(datastore, type, query);
        } else {
            item = new MorphiaQuery<>(datastore, null, type);
        }

        return item;
    }
    @Override
    public <T> Query<T> createQuery(Datastore datastore) {
        return new MorphiaQuery<>(datastore);
    }
}
