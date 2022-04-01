package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.updates.UpdateOperator;
import org.bson.Document;

import java.util.List;

/**
 * Defines an update operation
 *
 * @param <T>
 * @deprecated
 */
@Deprecated(since = "2.3")
public class Update<T> extends UpdateBase<T> {
    @SuppressWarnings("rawtypes")
    Update(Datastore datastore, MongoCollection<T> collection,
           Query<T> query, Class<T> type, UpdateOpsImpl operations) {
        super(datastore, collection, query, type, operations.getUpdates());
    }

    Update(Datastore datastore, MongoCollection<T> collection,
           Query<T> query, Class<T> type, List<UpdateOperator> updates) {
        super(datastore, collection, query, type, updates);
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
        String alternate = options.collection();
        MongoCollection<T> mongoCollection = alternate == null
                                             ? getCollection()
                                             : getDatastore()
                                                 .getDatabase()
                                                 .getCollection(alternate,
                                                     getCollection().getDocumentClass());

        if (options.isMulti()) {
            return session == null ? mongoCollection.updateMany(queryObject, updateOperations, options)
                                   : mongoCollection.updateMany(session, queryObject, updateOperations, options);

        } else {
            return session == null ? mongoCollection.updateOne(queryObject, updateOperations, options)
                                   : mongoCollection.updateOne(session, queryObject, updateOperations, options);
        }
    }
}
