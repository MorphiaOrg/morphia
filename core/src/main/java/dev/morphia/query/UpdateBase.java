package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @param <T>
 * @morphia.internal
 */
public abstract class UpdateBase<T> {

    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final Mapper mapper;
    private final Class<T> type;
    private final List<UpdateOperator> updates = new ArrayList<>();
    private final Datastore datastore;

    UpdateBase(Datastore datastore,
               Mapper mapper,
               @Nullable MongoCollection<T> collection,
               @Nullable Query<T> query,
               Class<T> type) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.collection = collection;
        this.query = query;
        this.type = type;
    }

    UpdateBase(Datastore datastore,
               Mapper mapper,
               MongoCollection<T> collection,
               Query<T> query,
               Class<T> type,
               UpdateOperator first,
               UpdateOperator[] updates) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.type = type;
        this.updates.add(first);
        this.updates.addAll(asList(updates));
        this.query = query;
        this.collection = collection;
    }

    UpdateBase(Datastore datastore,
               Mapper mapper,
               MongoCollection<T> collection,
               Query<T> query,
               Class<T> type,
               List<UpdateOperator> updates) {
        this(datastore, mapper, collection, query, type);
        this.updates.addAll(updates);
    }

    /**
     * Adds a new operator to this update operation.
     *
     * @param operator the new operator
     * @since 2.2
     */
    public void add(UpdateOperator operator) {
        updates.add(operator);
    }

    /**
     * @return the operations listed
     */
    public Document toDocument() {
        final Operations operations = new Operations(mapper, mapper.getEntityModel(type));

        for (UpdateOperator update : updates) {
            PathTarget pathTarget = new PathTarget(mapper, mapper.getEntityModel(type), update.field(), true);
            operations.add(update.operator(), update.toTarget(pathTarget));
        }
        return operations.toDocument();
    }

    @Override
    public String toString() {
        return toDocument().toString();
    }

    protected MongoCollection<T> getCollection() {
        return collection;
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    protected Query<T> getQuery() {
        return query;
    }

    /**
     * @return the updates
     * @morphia.internal
     */
    protected List<UpdateOperator> getUpdates() {
        return updates;
    }
}
