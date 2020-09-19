package dev.morphia.query;

import dev.morphia.Datastore;

/**
 * An abstract implementation of {@link QueryFactory}.
 */
public abstract class AbstractQueryFactory implements QueryFactory {
    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type) {
        return createQuery(datastore, type, null);
    }
}
