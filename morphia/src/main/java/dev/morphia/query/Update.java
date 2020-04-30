package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.Document;

/**
 * Defines an update operation
 *
 * @param <T>
 */
public class Update<T> extends UpdateBase<T> {
    @SuppressWarnings("rawtypes")
    Update(final Datastore datastore, final Mapper mapper, final MongoCollection<T> collection,
           final Query<T> query, final Class<T> type, final UpdateOpsImpl operations) {
        super(datastore, mapper, collection, query, type, operations.getUpdates());
    }

    Update(final Datastore datastore, final Mapper mapper, final MongoCollection<T> collection,
           final Query<T> query, final Class<T> type, final UpdateOperator first, final UpdateOperator[] updates) {
        super(datastore, mapper, collection, query, type, first, updates);
    }

    /**
     * Executes the update
     *
     * @return the results
     */
    public UpdateResult execute() {
        return execute(new UpdateOptions());
    }

    /**
     * Executes the update
     *
     * @param options the options to apply
     * @return the results
     */
    public UpdateResult execute(final UpdateOptions options) {
        Document updateOperations = toDocument();
        final Document queryObject = getQuery().toDocument();

        ClientSession session = getDatastore().findSession(options);
        MongoCollection<T> mongoCollection = options.prepare(getCollection());
        if (options.isMulti()) {
            return session == null ? mongoCollection.updateMany(queryObject, updateOperations, options)
                                   : mongoCollection.updateMany(session, queryObject, updateOperations, options);

        } else {
            return session == null ? mongoCollection.updateOne(queryObject, updateOperations, options)
                                   : mongoCollection.updateOne(session, queryObject, updateOperations, options);
        }
    }
}
