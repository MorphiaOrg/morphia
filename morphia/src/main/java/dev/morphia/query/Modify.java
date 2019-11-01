package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 */
public class Modify<T> extends UpdateBase<T, Modify<T>> {
    private final QueryImpl<T> query;
    private final MongoCollection<T> collection;
    private final Document queryObject;

    Modify(final QueryImpl<T> query) {
        super(query.getDatastore(), query.getDatastore().getMapper(), query.getEntityClass());
        this.query = query;
        this.collection = query.getCollection();
        this.queryObject = query.getQueryDocument();
    }

    /**
     * Performs the operation
     *
     * @return the operation result
     */
    public T execute() {
        return execute(new FindOneAndUpdateOptions()
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
    public T execute(final FindOneAndUpdateOptions options) {
        return collection.findOneAndUpdate(queryObject, toDocument(), options);

    }
}
