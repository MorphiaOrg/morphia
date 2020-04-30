package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
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

    UpdateBase(final Datastore datastore,
               final Mapper mapper,
               final MongoCollection<T> collection,
               final Query<T> query,
               final Class<T> type) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.collection = collection;
        this.query = query;
        this.type = type;
    }

    UpdateBase(final Datastore datastore,
               final Mapper mapper,
               final MongoCollection<T> collection,
               final Query<T> query,
               final Class<T> type,
               final UpdateOperator first,
               final UpdateOperator[] updates) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.type = type;
        this.updates.add(first);
        this.updates.addAll(asList(updates));
        this.query = query;
        this.collection = collection;
    }

    UpdateBase(final Datastore datastore,
               final Mapper mapper,
               final MongoCollection<T> collection,
               final Query<T> query,
               final Class<T> type,
               final List<UpdateOperator> updates) {
        this(datastore, mapper, collection, query, type);
        this.updates.addAll(updates);
    }

    /**
     * @return the operations listed
     */
    public Document toDocument() {
        final MappedClass mc = mapper.getMappedClass(type);
        final Operations operations = new Operations(mapper, mc);

        for (final UpdateOperator update : updates) {
            PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(type), update.field(), true);
            operations.add(update.operator(), update.toTarget(pathTarget));
        }
        return operations.toDocument();
    }

    protected void add(final UpdateOperator operator) {
        updates.add(operator);
    }

    protected MongoCollection<T> getCollection() {
        return collection;
    }

    @Override
    public String toString() {
        return toDocument().toString();
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
