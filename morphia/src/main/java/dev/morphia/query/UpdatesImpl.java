package dev.morphia.query;

import dev.morphia.Datastore;
import dev.morphia.UpdateDocument;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.List;

import static dev.morphia.UpdateDocument.Mode.BODY_ONLY;
import static dev.morphia.utils.ReflectionUtils.iterToList;
import static java.util.Collections.singletonList;

/**
 * @morphia.internal
 * @param <T>
 * @param <Updater>
 */
@SuppressWarnings("unchecked")
public abstract class UpdatesImpl<T, Updater extends Updates> implements Updates<Updater>  {

    protected Datastore datastore;
    protected final Mapper mapper;
    protected final Class<T> clazz;
    private final Operations operations = new Operations();
    private boolean validateNames = true;
    private boolean versionedUpdate;
    private UpdateDocument updateDocument;

    UpdatesImpl(final Datastore datastore, final Mapper mapper, final Class<T> clazz) {
        this.datastore = datastore;
        this.mapper = mapper;
        this.clazz = clazz;
    }

    @Override
    public Updater addToSet(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.ADD_TO_SET, field, value);
        return (Updater)this;
    }

    @Override
    public Updater addToSet(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new UpdateException("Values cannot be null or empty.");
        }
        PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateNames);

        addOperation(UpdateOperator.ADD_TO_SET_EACH, pathTarget, new Document(UpdateOperator.EACH.val(), values));
        return (Updater)this;
    }

    @Override
    public Updater addToSet(final String field, final Iterable<?> values) {
        return addToSet(field, iterToList(values));
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
        addOperation(UpdateOperator.PUSH, new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateNames), document);

        return (Updater)this;
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
        return (Updater)this;
    }

    @Override
    public Updater enableValidation() {
        validateNames = true;
        return (Updater)this;
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
        return (Updater)this;
    }

    @Override
    public Updater max(final String field, final Number value) {
        add(UpdateOperator.MAX, field, value);
        return (Updater)this;
    }

    @Override
    public Updater min(final String field, final Number value) {
        add(UpdateOperator.MIN, field, value);
        return (Updater)this;
    }

    @Override
    public Updater removeAll(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }
        add(UpdateOperator.PULL, field, value);
        return (Updater)this;
    }

    @Override
    public Updater removeAll(final String field, final List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new QueryException("Value cannot be null or empty.");
        }

        add(UpdateOperator.PULL_ALL, field, values);
        return (Updater)this;
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
        return (Updater)this;
    }

    @Override
    public Updater set(final Object entity) {
        if (entity == null) {
            throw new QueryException("Entity value cannot be null.");
        }

        updateDocument = new UpdateDocument(entity, BODY_ONLY);
        if(versionedUpdate) {
            updateDocument.skipVersion();
        }
        operations.add(UpdateOperator.SET, new TargetValue(null, updateDocument));
        return (Updater) this;
    }

    @Override
    public Updater setOnInsert(final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Value cannot be null.");
        }

        add(UpdateOperator.SET_ON_INSERT, field, value);
        return (Updater)this;
    }

    @Override
    public Updater unset(final String field) {
        addOperation(UpdateOperator.UNSET, new PathTarget(mapper, clazz, field, validateNames), null);
        return (Updater)this;
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
        if (1 == 1) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        }

//        this.ops = ops;
    }

    private void add(final UpdateOperator op, final String field, final Object value) {
        if (value == null) {
            throw new QueryException("Val cannot be null");
        }

        addOperation(op, new PathTarget(mapper, clazz, field, validateNames), value);
    }

    protected Updater remove(final String fieldExpr, final boolean firstNotLast) {
        add(UpdateOperator.POP, fieldExpr, (firstNotLast) ? -1 : 1);
        return (Updater)this;
    }

    @Override
    public String toString() {
        return toDocument().toString();
    }

    private void addOperation(final UpdateOperator operator, final PathTarget path, final Object val) {
        operations.add(operator, new TargetValue(path, val));
    }

    protected void versionUpdate() {
        final MappedClass mc = mapper.getMappedClass(clazz);

        if (mc.getVersionField() != null) {
            versionedUpdate = true;
            if(updateDocument != null) {
                updateDocument.skipVersion();
            }
            inc(mc.getVersionField().getMappedFieldName());
        }
    }

}
