package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @param <T>
 * @param <O>
 * @morphia.internal
 */
public abstract class UpdateBase<T, O> {

    private final List<O> updates = new ArrayList<>();
    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final Mapper mapper;
    private final Class<T> type;
    private final Datastore datastore;

    UpdateBase(Datastore datastore,
               @Nullable MongoCollection<T> collection,
               @Nullable Query<T> query,
               Class<T> type,
               List<O> updates) {
        this.datastore = datastore;
        this.mapper = datastore.getMapper();
        this.collection = collection;
        this.query = query;
        this.type = type;
        this.updates.addAll(updates);
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
    public void add(O operator) {
        updates.add(operator);
    }

    /**
     * Executes the update
     *
     * @param options the options to apply
     * @return the results
     */
    public abstract UpdateResult execute(UpdateOptions options);

    public UpdateResult execute() {
        return execute(new UpdateOptions());
    }

    public Mapper getMapper() {
        return mapper;
    }

    public Class<T> getType() {
        return type;
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

    protected List<O> getUpdates() {
        return updates;
    }
}
