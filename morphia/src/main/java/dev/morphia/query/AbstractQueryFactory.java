package dev.morphia.query;

import dev.morphia.Datastore;

/**
 * An abstract implementation of {@link QueryFactory}.
 */
public abstract class AbstractQueryFactory implements QueryFactory {

    @Override
    public <T> Query<T> createQuery(final Datastore datastore, final Class<T> type) {
        return createQuery(datastore, type, null);
    }

    @Override
    public <T> Query<T> createQuery(final Datastore datastore) {
        return new LegacyQuery<>(null, datastore);
    }
}
