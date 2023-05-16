package dev.morphia.query;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;

import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A implementation of {@link QueryFactory} to create {@link LegacyQuery} instances.
 *
 * @deprecated
 */
@Deprecated
public class LegacyQueryFactory implements QueryFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyQueryFactory.class);

    /**
     * Logs a message stating this (obscured/hidden) class is going away and to read the website for steps on how to migrate away from it.
     */
    public LegacyQueryFactory() {
        Sofia.logLegacyQuery();
    }

    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type, @Nullable Document seed) {

        final LegacyQuery<T> query = new LegacyQuery<>(datastore, null, type);

        if (seed != null) {
            query.setQueryObject(seed);
        }

        return query;
    }

    @Override
    public <T> Query<T> createQuery(Datastore datastore, String collection, Class<T> type) {
        return new LegacyQuery<>(datastore, collection, type);
    }
}
