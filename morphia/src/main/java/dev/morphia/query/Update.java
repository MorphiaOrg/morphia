package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DatastoreImpl;
import dev.morphia.UpdateOptions;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

/**
 * Defines an update operation
 *
 * @param <T>
 */
public class Update<T> extends UpdateBase<T, Update<T>> {
    private QueryImpl query;
    private MongoCollection<T> collection;

    Update(final DatastoreImpl datastore, final Mapper mapper, final Class<T> clazz, final MongoCollection<T> collection,
           final QueryImpl query) {
        super(datastore, mapper, clazz);
        this.collection = collection;
        this.query = query;
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
        MongoCollection mongoCollection = getDatastore().enforceWriteConcern(collection, getType(), options.getWriteConcern());
        Document updateOperations = toDocument();
        final Document queryObject = query.prepareQuery();
        ClientSession session = getDatastore().findSession(options);
        if (options.isMulti()) {
            return session == null ? mongoCollection.updateMany(queryObject, updateOperations, options)
                                   : mongoCollection.updateMany(session, queryObject, updateOperations, options);

        } else {
            return session == null ? mongoCollection.updateOne(queryObject, updateOperations, options)
                                   : mongoCollection.updateOne(session, queryObject, updateOperations, options);
        }
    }
}
