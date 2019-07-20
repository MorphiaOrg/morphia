package dev.morphia.query;

import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A default implementation of {@link QueryFactory}.
 */
public class DefaultQueryFactory extends AbstractQueryFactory {

    @Override
    public <T> Query<T> createQuery(final Datastore datastore, final Class<T> type, final Document query) {

        final QueryImpl<T> item = new QueryImpl<>(type, datastore);

        if (query != null) {
            item.setQueryObject(query);
        }

        return item;
    }

}
