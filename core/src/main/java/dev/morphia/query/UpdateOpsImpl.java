package dev.morphia.query;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.PopOperator;
import dev.morphia.query.experimental.updates.PushOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @param <T> the type to update
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public class UpdateOpsImpl<T> extends UpdateBase<T> implements UpdateOperations<T> {
    private Document ops = new Document();
    private boolean validateNames = true;

    /**
     * Creates an UpdateOpsImpl for the type given.
     *
     * @param datastore the datastore to use
     * @param type      the type to update
     * @param mapper    the Mapper to use
     */
    public UpdateOpsImpl(Datastore datastore, Class<T> type, Mapper mapper) {
        super(datastore, mapper, null, null, type);
    }

    static <T> List<T> iterToList(Iterable<T> it) {
        if (it instanceof List) {
            return (List<T>) it;
        }

        final List<T> ar = new ArrayList<>();
        for (T o : it) {
            ar.add(o);
        }

        return ar;
    }

    @Override
    public UpdateOperations<T> addToSet(String field, Object value) {
        add(UpdateOperators.addToSet(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> addToSet(String field, List<?> values) {
        add(UpdateOperators.addToSet(field, values));
        return this;
    }

    @Override
    public UpdateOperations<T> addToSet(String field, Iterable<?> values) {
        return addToSet(field, iterToList(values));
    }

    @Override
    public UpdateOperations<T> dec(String field) {
        return inc(field, -1);
    }

    @Override
    public UpdateOperations<T> dec(String field, Number value) {
        if ((value instanceof Long) || (value instanceof Integer)) {
            return inc(field, (value.longValue() * -1));
        }
        if ((value instanceof Double) || (value instanceof Float)) {
            return inc(field, (value.doubleValue() * -1));
        }
        throw new IllegalArgumentException(
            "Currently only the following types are allowed: integer, long, double, float.");
    }

    @Override
    public UpdateOperations<T> disableValidation() {
        validateNames = false;
        return this;
    }

    @Override
    public UpdateOperations<T> enableValidation() {
        validateNames = true;
        return this;
    }

    @Override
    public UpdateOperations<T> inc(String field) {
        return inc(field, 1);
    }

    @Override
    public UpdateOperations<T> inc(String field, Number value) {
        add(UpdateOperators.inc(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> max(String field, Number value) {
        add(UpdateOperators.max(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> min(String field, Number value) {
        add(UpdateOperators.min(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> push(String field, Object value) {
        add(UpdateOperators.push(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> push(String field, Object value, PushOptions options) {
        PushOperator push = UpdateOperators.push(field, value);
        options.update(push);

        add(push);
        return this;
    }

    @Override
    public UpdateOperations<T> push(String field, List<?> values) {
        add(UpdateOperators.push(field, values));
        return this;
    }

    @Override
    public UpdateOperations<T> push(String field, List<?> values, PushOptions options) {
        PushOperator push = UpdateOperators.push(field, values);
        options.update(push);

        add(push);
        return this;
    }

    @Override
    public UpdateOperations<T> removeAll(String field, Object value) {
        add(UpdateOperators.pull(field, Filters.eq(field, value)));
        return this;
    }

    @Override
    public UpdateOperations<T> removeAll(String field, List<?> values) {
        add(UpdateOperators.pullAll(field, values));
        return this;
    }

    @Override
    public UpdateOperations<T> removeFirst(String field) {
        return remove(field, true);
    }

    @Override
    public UpdateOperations<T> removeLast(String field) {
        return remove(field, false);
    }

    @Override
    public UpdateOperations<T> set(String field, Object value) {
        add(UpdateOperators.set(field, value));
        return this;
    }

    @Override
    public UpdateOperations<T> setOnInsert(String field, Object value) {
        add(UpdateOperators.setOnInsert(Map.of(field, value)));
        return this;
    }

    @Override
    public UpdateOperations<T> unset(String field) {
        add(UpdateOperators.unset(field));
        return this;
    }

    /**
     * @return the operations listed
     */
    public Document getOps() {
        return new Document(ops);
    }

    /**
     * Sets the operations for this UpdateOpsImpl
     *
     * @param ops the operations
     */
    public void setOps(Document ops) {
        this.ops = ops;
    }

    protected UpdateOperations<T> remove(String fieldExpr, boolean firstNotLast) {
        PopOperator pop = UpdateOperators.pop(fieldExpr);
        if (firstNotLast) {
            pop.removeFirst();
        }
        add(pop);
        return this;
    }
}
