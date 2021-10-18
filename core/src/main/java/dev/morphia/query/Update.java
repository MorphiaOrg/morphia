package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.Document;

/**
 * Defines an update operation
 *
 * @param <T>
 */
public class Update<T> extends UpdateBase<T> {
    @SuppressWarnings("rawtypes")
    Update(Datastore datastore, MongoCollection<T> collection,
           Query<T> query, Class<T> type, UpdateOpsImpl operations) {
        super(datastore, collection, query, type, operations.getUpdates());
    }

    Update(Datastore datastore, MongoCollection<T> collection,
           Query<T> query, Class<T> type, UpdateOperator first, UpdateOperator[] updates) {
        super(datastore, collection, query, type, first, updates);
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
    public UpdateResult execute(UpdateOptions options) {
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
