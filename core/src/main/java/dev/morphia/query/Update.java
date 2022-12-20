package dev.morphia.query;

import java.util.List;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

import dev.morphia.DatastoreImpl;
import dev.morphia.UpdateOptions;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;

/**
 * Defines an update operation
 *
 * @param <T>
 * @deprecated
 */
@Deprecated(since = "2.3")
public class Update<T> extends UpdateBase<T> {
    @SuppressWarnings("rawtypes")
    Update(DatastoreImpl datastore, MongoCollection<T> collection,
            Query<T> query, Class<T> type, UpdateOpsImpl operations) {
        super(datastore, collection, query, type, operations.getUpdates());
    }

    Update(DatastoreImpl datastore, MongoCollection<T> collection,
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
        this.validateDocument = options.getBypassDocumentValidation() == null || !options.getBypassDocumentValidation();
        Document updateOperations = toDocument();
        final Document queryObject = getQuery().toDocument();
        if (options.isUpsert()) {
            EntityModel entityModel = getDatastore().getMapper().getEntityModel(getQuery().getEntityClass());
            if (entityModel.useDiscriminator()) {
                queryObject.put(entityModel.getDiscriminatorKey(), entityModel.getDiscriminator());
            }
        }

        MongoCollection<T> mongoCollection = options.prepare(getCollection(), getDatastore().getDatabase());

        if (options.multi()) {
            return getDatastore().operations().updateMany(mongoCollection, queryObject, updateOperations, options);
        } else {
            return getDatastore().operations().updateOne(mongoCollection, queryObject, updateOperations, options);
        }
    }
}
