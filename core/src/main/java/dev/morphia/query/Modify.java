package dev.morphia.query;

import java.util.List;

import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;

import dev.morphia.DatastoreImpl;
import dev.morphia.ModifyOptions;
import dev.morphia.query.internal.DeferredDecoding;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 * @deprecated
 */
@Deprecated(since = "2.3")
public class Modify<T> extends UpdateBase<T> {

    @SuppressWarnings("rawtypes")
    Modify(DatastoreImpl datastore, MongoCollection<T> collection, Query<T> query, Class<T> type,
            UpdateOpsImpl operations) {
        super(datastore, collection, query, type, operations.getUpdates());

    }

    Modify(DatastoreImpl datastore, MongoCollection<T> collection, Query<T> query, Class<T> type,
            List<UpdateOperator> updates) {
        super(datastore, collection, query, type, updates);
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
        MongoCollection<T> collection = getDatastore().configureCollection(options, getCollection());
        Document update = toDocument();

        if (DeferredDecoding.required(getDatastore())) {
            return DeferredDecoding.decode(collection.getCodecRegistry(), collection.getDocumentClass(),
                    getDatastore().operations().findOneAndUpdate(DeferredDecoding.raw(collection), getQuery().toDocument(), update,
                            options));
        }
        return getDatastore().operations().findOneAndUpdate(collection, getQuery().toDocument(), update, options);
    }
}
