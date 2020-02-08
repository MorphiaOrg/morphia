package dev.morphia.query;

import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A implementation of {@link QueryFactory} to create {@link LegacyQuery} instances.
 */
public class LegacyQueryFactory extends AbstractQueryFactory {

    @Override
    public <T> Query<T> createQuery(final Datastore datastore, final Class<T> type, final Document query) {

        final LegacyQuery<T> item = new LegacyQuery<>(type, datastore);

        if (query != null) {
            item.setQueryObject(query);
        }

        return item;
    }

}
