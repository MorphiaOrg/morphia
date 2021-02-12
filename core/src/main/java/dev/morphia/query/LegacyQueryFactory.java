package dev.morphia.query;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import org.bson.Document;

/**
 * A implementation of {@link QueryFactory} to create {@link LegacyQuery} instances.
 */
public class LegacyQueryFactory implements QueryFactory {
    @Override
    public <T> Query<T> createQuery(Datastore datastore, String collection, Class<T> type) {
        return new LegacyQuery<>(datastore, collection, type);
    }

    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type, @Nullable Document seed) {

        final LegacyQuery<T> query = new LegacyQuery<>(datastore, null, type);

        if (seed != null) {
            query.setQueryObject(seed);
        }

        return query;
    }
}
