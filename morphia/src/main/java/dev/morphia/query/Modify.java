package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
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
    Modify(final Datastore datastore, final Mapper mapper, final MongoCollection<T> collection, final Query<T> query, final Class<T> type,
           final UpdateOpsImpl operations) {
        super(datastore, mapper, collection, query, type, operations.getUpdates());

    }

    Modify(final Datastore datastore, final Mapper mapper, final MongoCollection<T> collection, final Query<T> query, final Class<T> type,
           final UpdateOperator first, final UpdateOperator[] updates) {
        super(datastore, mapper, collection, query, type, first, updates);
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    public T execute() {
        return execute(new ModifyOptions());
    }

    /**
     * Performs the operation
     *
     * @param options the options to apply
     * @return the operation result
     */
    public T execute(final ModifyOptions options) {
        ClientSession session = getDatastore().findSession(options);
        Document update = toDocument();

        return session == null
               ? options.prepare(getCollection()).findOneAndUpdate(getQuery().toDocument(), update, options)
               : options.prepare(getCollection()).findOneAndUpdate(session, getQuery().toDocument(), update, options);
    }
}
