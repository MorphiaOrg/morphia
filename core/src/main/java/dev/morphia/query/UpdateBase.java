package dev.morphia.query;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;

import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.internal.DatastoreAware;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import static java.util.Arrays.asList;

/**
 * @param <T>
 * @morphia.internal
 */
@MorphiaInternal
public abstract class UpdateBase<T> {

    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final Mapper mapper;
    private final Class<T> type;
    private final List<UpdateOperator> updates = new ArrayList<>();
    private final DatastoreImpl datastore;

    UpdateBase(DatastoreImpl datastore,
            @Nullable MongoCollection<T> collection,
            @Nullable Query<T> query,
            Class<T> type,
            List<UpdateOperator> updates) {
        this.datastore = datastore;
        this.mapper = datastore.getMapper();
        this.type = type;
        this.updates.addAll(updates);
        this.query = query;
        this.collection = collection;
    }

    @NotNull
    static <T> List<T> coalesce(T first, T[] updates) {
        List<T> operators = new ArrayList<>();
        operators.add(first);
        operators.addAll(asList(updates));
        return operators;
    }

    /**
     * Adds a new operator to this update operation.
     *
     * @param operator the new operator
     * @morphia.internal
     * @since 2.2
     */
    @MorphiaInternal
    public void add(UpdateOperator operator) {
        updates.add(operator);
    }

    /**
     * @return the operations listed
     * @morphia.internal
     */
    public Document toDocument() {
        final Operations operations = new Operations(datastore, mapper.getEntityModel(type));

        for (UpdateOperator update : updates) {
            PathTarget pathTarget = new PathTarget(mapper, mapper.getEntityModel(type), update.field(), validate());
            if (update instanceof DatastoreAware) {
                ((DatastoreAware) update).setDatastore(datastore);
            }
            operations.add(update.operator(), update.toTarget(pathTarget));
        }
        return operations.toDocument();
    }

    private boolean validate() {
        return query instanceof MorphiaQuery
                ? ((MorphiaQuery) query).isValidate()
                : ((LegacyQuery) query).isValidate();
    }

    @Override
    public String toString() {
        return toDocument().toString();
    }

    protected MongoCollection<T> getCollection() {
        return collection;
    }

    protected DatastoreImpl getDatastore() {
        return datastore;
    }

    protected Query<T> getQuery() {
        return query;
    }

    /**
     * @return the updates
     * @morphia.internal
     */
    @MorphiaInternal
    protected List<UpdateOperator> getUpdates() {
        return updates;
    }
}
