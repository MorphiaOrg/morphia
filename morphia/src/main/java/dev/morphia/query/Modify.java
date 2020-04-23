package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.Datastore;
import dev.morphia.ModifyOptions;
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
        super(datastore, mapper, type);
        this.query = query;
        this.collection = collection;
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    public T execute() {
        return execute(new ModifyOptions()
                           .returnDocument(ReturnDocument.AFTER));
    }

    /**
     * Performs the operation
     *
     * @param options the options to apply
     * @return the operation result
     */
    public T execute(final ModifyOptions options) {
        ClientSession session = getDatastore().findSession(options);

        return session == null
               ? options.prepare(collection).findOneAndUpdate(query.toDocument(), toDocument(), options)
               : options.prepare(collection).findOneAndUpdate(session, query.toDocument(), toDocument(), options);
    }
}
