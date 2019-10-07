package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.List;

public class Update<T> extends UpdatesImpl<T, Update<T>> {
    private Document queryObject;
    private MongoCollection<T> collection;

    Update(final Datastore datastore, final Mapper mapper, final Class<T> clazz, final MongoCollection<T> collection,
           final Document queryObject) {
        super(datastore, mapper, clazz);
        this.collection = collection;
        this.queryObject = queryObject;
    }

    public UpdateResult execute() {
        return execute(new UpdateOptions());
    }

    public UpdateResult execute(final UpdateOptions options) {

        final List<MappedField> fields = mapper.getMappedClass(clazz)
                                               .getFields(Version.class);
        if (!fields.isEmpty()) {
            inc(fields.get(0).getMappedFieldName(), 1);
        }
        MongoCollection mongoCollection = datastore.enforceWriteConcern(collection, clazz, options.getWriteConcern());
        versionUpdate();
        Document update = toDocument();
        return options.isMulti()
               ? mongoCollection.updateMany(queryObject, update, options)
               : mongoCollection.updateOne(queryObject, update, options);
    }

}
