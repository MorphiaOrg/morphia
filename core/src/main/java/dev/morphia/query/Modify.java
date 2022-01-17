package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.ModifyOptions;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a modify operation
 *
 * @param <T> the entity type
 */
public class Modify<T> {

    private final List<UpdateOperator> updates = new ArrayList<>();
    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final Mapper mapper;
    private final Class<T> type;
    private final Datastore datastore;

    @SuppressWarnings("rawtypes")
    Modify(Datastore datastore, MongoCollection<T> collection, Query<T> query, Class<T> type,
           UpdateOpsImpl operations) {
        this(datastore, collection, query, type, operations.getUpdates());
    }

    Modify(Datastore datastore, MongoCollection<T> collection, Query<T> query, Class<T> type, List<UpdateOperator> updates) {
        this.datastore = datastore;
        this.mapper = datastore.getMapper();
        this.collection = collection;
        this.query = query;
        this.type = type;
        this.updates.addAll(updates);
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
        ClientSession session = datastore.findSession(options);
        Document update = toDocument();

        return session == null
               ? options.prepare(collection).findOneAndUpdate(query.toDocument(), update, options)
               : options.prepare(collection).findOneAndUpdate(session, query.toDocument(), update, options);
    }

    /**
     * @return the operations listed
     */
    private Document toDocument() {
        final Operations operations = new Operations(datastore, mapper.getEntityModel(type));

        for (UpdateOperator update : updates) {
            PathTarget pathTarget = new PathTarget(mapper, mapper.getEntityModel(type), update.field(), true);
            operations.add(update.operator(), update.toTarget(pathTarget));
        }
        return operations.toDocument();
    }
}
