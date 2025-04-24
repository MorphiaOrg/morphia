package dev.morphia.query;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * A default implementation of {@link QueryFactory}.
 * 
 * @morphia.internal
 */
@MorphiaInternal
public class DefaultQueryFactory implements QueryFactory {
    @Override
    public <T> Query<T> createQuery(Datastore datastore, Class<T> type, FindOptions options, @Nullable Document query) {
        return new MorphiaQuery<>(datastore, type, options, query);
    }
}
