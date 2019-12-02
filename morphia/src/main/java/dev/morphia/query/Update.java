package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
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

    Update(final Datastore datastore, final Mapper mapper, final Class<T> clazz, final MongoCollection<T> collection,
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
        if(getDatastore().getSession() != null) {
            return options.isMulti()
                   ? mongoCollection.updateMany(getDatastore().getSession(), queryObject, updateOperations, options)
                   : mongoCollection.updateOne(getDatastore().getSession(), queryObject, updateOperations, options);

        } else {
            return options.isMulti()
                   ? mongoCollection.updateMany(queryObject, updateOperations, options)
                   : mongoCollection.updateOne(queryObject, updateOperations, options);
        }
    }
}
