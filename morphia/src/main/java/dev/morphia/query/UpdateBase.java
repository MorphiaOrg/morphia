package dev.morphia.query;

import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static java.util.Collections.singletonList;

/**
 * @param <T>
 * @param <Updater>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public abstract class UpdateBase<T, Updater extends Updates> implements Updates<Updater> {

    private final Mapper mapper;
    private final Class<T> type;
    private final Operations operations;
    private Datastore datastore;
    private boolean validateNames = true;

    UpdateBase(final Datastore datastore, final Mapper mapper, final Class<T> type) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.type = type;
        final MappedClass mc = mapper.getMappedClass(type);
        operations = new Operations(mapper, mc);
    }

    protected Class<T> getType() {
        return type;
    }

    protected Operations getOperations() {
        return operations;
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    @Override
    public Updater addToSet(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.ADD_TO_SET, field, value);
        return (Updater) this;
    }

    @Override
    public Updater addToSet(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new UpdateException("Values cannot be null or empty.");
        }
        PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(type), field, validateNames);

        addOperation(UpdateOperator.ADD_TO_SET_EACH, pathTarget, new Document(UpdateOperator.EACH.val(), values));
        return (Updater) this;
    }

    @Override
    public Updater addToSet(final String field, final Iterable<?> values) {
        return addToSet(field, iterToList(values));
    }

    @Override
    public Updater dec(final String field) {
        return inc(field, -1);
    }

    @Override
    public Updater dec(final String field, final Number value) {
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
    public Updater disableValidation() {
        validateNames = false;
        return (Updater) this;
    }

    @Override
    public Updater enableValidation() {
        validateNames = true;
        return (Updater) this;
    }

    @Override
    public Updater inc(final String field) {
        return inc(field, 1);
    }

    @Override
    public Updater inc(final String field, final Number value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }
        add(UpdateOperator.INC, field, value);
        return (Updater) this;
    }

    @Override
    public Updater max(final String field, final Number value) {
        add(UpdateOperator.MAX, field, value);
        return (Updater) this;
    }

    @Override
    public Updater min(final String field, final Number value) {
        add(UpdateOperator.MIN, field, value);
        return (Updater) this;
    }

    @Override
    public Updater pull(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }
        add(UpdateOperator.PULL, field, value);
        return (Updater) this;
    }

    @Override
    public Updater pullAll(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Value cannot be null or empty.");
        }

        add(UpdateOperator.PULL_ALL, field, values);
        return (Updater) this;
    }

    @Override
    public Updater push(final String field, final Object value) {
        return push(field, value instanceof List ? (List<?>) value : singletonList(value), new PushOptions());
    }

    @Override
    public Updater push(final String field, final Object value, final PushOptions options) {
        return push(field, value instanceof List ? (List<?>) value : singletonList(value), options);
    }

    @Override
    public Updater push(final String field, final List<?> values) {
        return push(field, values, new PushOptions());
    }

    @Override
    public Updater push(final String field, final List<?> values, final PushOptions options) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Values cannot be null or empty.");
        }

        Document document = new Document(UpdateOperator.EACH.val(), values);
        options.update(document);
        addOperation(UpdateOperator.PUSH, new PathTarget(mapper, mapper.getMappedClass(type), field, validateNames), document);

        return (Updater) this;
    }

    @Override
    public Updater removeAll(final String field, final Object value) {
        return pull(field, value);
    }

    @Override
    public Updater removeAll(final String field, final List<?> values) {
        return pullAll(field, values);
    }

    @Override
    public Updater removeFirst(final String field) {
        return remove(field, true);
    }

    @Override
    public Updater removeLast(final String field) {
        return remove(field, false);
    }

    @Override
    public Updater set(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value for field [" + field + "] cannot be null.");
        }

        add(UpdateOperator.SET, field, value);
        return (Updater) this;
    }

    @Override
    public Updater set(final Object entity) {
        operations.replaceEntity(entity);
        return (Updater) this;
    }

    @Override
    public Updater setOnInsert(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.SET_ON_INSERT, field, value);
        return (Updater) this;
    }

    @Override
    public Updater unset(final String field) {
        addOperation(UpdateOperator.UNSET, new PathTarget(mapper, type, field, validateNames), null);
        return (Updater) this;
    }

    protected Updater remove(final String fieldExpr, final boolean firstNotLast) {
        add(UpdateOperator.POP, fieldExpr, (firstNotLast) ? -1 : 1);
        return (Updater) this;
    }

    /**
     * Converts an Iterable to a List
     *
     * @param it  the Iterable
     * @param <T> the types of the elements in the Iterable
     * @return the List
     */
    public static <T> List<T> iterToList(final Iterable<T> it) {
        if (it instanceof List) {
            return (List<T>) it;
        }
        if (it == null) {
            return null;
        }

        final List<T> ar = new ArrayList<>();
        for (final T o : it) {
            ar.add(o);
        }

        return ar;
    }

    private void add(final UpdateOperator op, final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Val cannot be null");
        }

        addOperation(op, new PathTarget(mapper, type, field, validateNames), value);
    }

    private void addOperation(final UpdateOperator operator, final PathTarget path, final Object val) {
        operations.add(operator, new OperationTarget(path, val));
    }

    @Override
    public String toString() {
        return toDocument().toString();
    }

    /**
     * @return the operations listed
     */
    public Document toDocument() {
        return operations.toDocument();
    }

    /**
     * Sets the operations for this UpdateOpsImpl
     *
     * @param ops the operations
     */
    void setOps(final Document ops) {
        for (final Entry<String, Object> entry : ops.entrySet()) {
            Document value = (Document) entry.getValue();
            UpdateOperator op = UpdateOperator.fromString(entry.getKey());
            for (final Entry<String, Object> valueEntry : value.entrySet()) {
                add(op, valueEntry.getKey(), valueEntry.getValue());
            }
        }

        //        this.ops = ops;
    }
}
