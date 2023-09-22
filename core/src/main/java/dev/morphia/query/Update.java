package dev.morphia.query;

import java.util.List;

import com.mongodb.client.MongoCollection;

import dev.morphia.DatastoreImpl;
import dev.morphia.query.updates.UpdateOperator;

/**
 * Defines an update operation
 *
 * @param <T>
 * @hidden
 */
public class Update<T> extends UpdateBase<T> {

    public Update(DatastoreImpl datastore, MongoCollection<T> collection,
            MorphiaQuery<T> query, Class<T> type, List<UpdateOperator> updates) {
        super(datastore, collection, query, type, updates);
    }

}
