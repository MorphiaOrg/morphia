package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

public class Modify<T> extends UpdateBase<T, Modify<T>> {
    private final QueryImpl<T> query;
    private final MongoCollection<T> collection;
    private final Document queryObject;

    Modify(final QueryImpl<T> query) {
        super(query.datastore, query.mapper, query.clazz);
        this.query = query;
        this.collection = query.getCollection();
        this.queryObject = query.getQueryDocument();
    }

    public T execute() {
        return execute(new FindOneAndUpdateOptions()
                           .returnDocument(ReturnDocument.AFTER)
                           .sort(query.getSort())
                           .projection(query.getFieldsObject()));
    }

    public T execute(final FindOneAndUpdateOptions options) {
        return collection.findOneAndUpdate(queryObject, toDocument(), options);

    }
}
