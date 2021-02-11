package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.ModifyOptions;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.Document;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 */
public class Modify<T> extends UpdateBase<T> {

    @SuppressWarnings("rawtypes")
    Modify(Datastore datastore, Mapper mapper, MongoCollection<T> collection, Query<T> query, Class<T> type,
           UpdateOpsImpl operations) {
        super(datastore, mapper, collection, query, type, operations.getUpdates());

    }

    Modify(Datastore datastore, Mapper mapper, MongoCollection<T> collection, Query<T> query, Class<T> type,
           UpdateOperator first, UpdateOperator[] updates) {
        super(datastore, mapper, collection, query, type, first, updates);
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    @Nullable
    public T execute() {
        return execute(new ModifyOptions());
    }

    /**
     * Performs the operation
     *
     * @param options the options to apply
     * @return the operation result
     */
    @Nullable
    public T execute(ModifyOptions options) {
        ClientSession session = getDatastore().findSession(options);
        Document update = toDocument();

        return session == null
               ? options.prepare(getCollection()).findOneAndUpdate(getQuery().toDocument(), update, options)
               : options.prepare(getCollection()).findOneAndUpdate(session, getQuery().toDocument(), update, options);
    }
}
