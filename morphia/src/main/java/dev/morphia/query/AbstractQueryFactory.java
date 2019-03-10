package dev.morphia.query;

import com.mongodb.DBCollection;
import dev.morphia.Datastore;

/**
 * An abstract implementation of {@link QueryFactory}.
 */
public abstract class AbstractQueryFactory implements QueryFactory {

    @Override
    public <T> Query<T> createQuery(final Datastore datastore, final DBCollection collection, final Class<T> type) {
        return createQuery(datastore, collection, type, null);
    }

    @Override
    public <T> Query<T> createQuery(final Datastore datastore) {
        return new QueryImpl<T>(null, null, datastore);
    }
}
