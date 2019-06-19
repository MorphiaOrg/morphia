package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedClass;
import org.bson.Document;

public class Modify<T> extends UpdatesImpl<Modify<T>> {
    private final QueryImpl<T> query;
    private final MongoCollection<T> collection;
    private final Document queryObject;

    Modify(final QueryImpl<T> query) {
        super(query.ds, query.mapper, query.clazz);
        this.query = query;
        this.collection = query.collection;
        this.queryObject = query.getQueryDocument();
    }

    public T execute() {
        return execute(new FindOneAndUpdateOptions()
                           .sort(query.getSort())
                           .projection(query.getFieldsObject()));
    }

    public T execute(final FindOneAndUpdateOptions options) {
        versionUpdate();
        Document res = (Document) collection.findOneAndUpdate(queryObject, getOps(), options);

        return mapper.fromDocument(datastore, query.getEntityClass(), res, mapper.createEntityCache());

    }

    private void versionUpdate() {
        final MappedClass mc = mapper.getMappedClass(clazz);

        if (!mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            inc(mc.getMappedVersionField().getNameToStore());
        }

    }
}
