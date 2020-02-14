package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.FindAndModifyOptions;
import dev.morphia.mapping.Mapper;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 */
public class Modify<T> extends UpdateBase<T, Modify<T>> {
    private final Query<T> query;
    private final MongoCollection<T> collection;

    Modify(final Query<T> query, final Datastore datastore, final Mapper mapper, final Class<T> type, final MongoCollection<T> collection) {
        super((DatastoreImpl) datastore, mapper, type);
        this.query = query;
        this.collection = collection;
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    public T execute() {
        return execute(new FindAndModifyOptions()
                           .returnDocument(ReturnDocument.AFTER));
    }

    /**
     * Performs the operation
     *
     * @param options the options to apply
     * @return the operation result
     */
    public T execute(final FindAndModifyOptions options) {
        ClientSession session = getDatastore().findSession(options);

        return session == null
               ? options.apply(collection).findOneAndUpdate(query.toDocument(), toDocument(), options)
               : options.apply(collection).findOneAndUpdate(session, query.toDocument(), toDocument(), options);
    }
}
