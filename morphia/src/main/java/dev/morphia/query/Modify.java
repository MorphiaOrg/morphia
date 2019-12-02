package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.DatastoreImpl;
import dev.morphia.FindAndModifyOptions;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 */
public class Modify<T> extends UpdateBase<T, Modify<T>> {
    private final QueryImpl<T> query;
    private final MongoCollection<T> collection;

    Modify(final QueryImpl<T> query) {
        super((DatastoreImpl) query.getDatastore(), query.getDatastore().getMapper(), query.getEntityClass());
        this.query = query;
        this.collection = query.getCollection();
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    public T execute() {
        return execute(new FindAndModifyOptions()
                           .returnDocument(ReturnDocument.AFTER)
                           .sort(query.getSort())
                           .projection(query.getFieldsObject()));
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
               ? options.apply(collection).findOneAndUpdate(query.prepareQuery(), toDocument(), options)
               : options.apply(collection).findOneAndUpdate(session, query.prepareQuery(), toDocument(), options);

    }
}
