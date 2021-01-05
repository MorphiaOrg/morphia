package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.annotations.Handler;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Wrapper type for references to entities in other collections
 *
 * @param <T>
 * @since 1.5
 */
@SuppressWarnings("unchecked")
@Handler(MorphiaReferenceCodec.class)
public abstract class MorphiaReference<T> {
    private Datastore datastore;
    private boolean ignoreMissing;
    private boolean resolved;

    MorphiaReference() {
    }

    MorphiaReference(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * Wraps an value in a MorphiaReference to storing on an entity
     *
     * @param value the value wrap
     * @param <V>   the type of the value
     * @return the MorphiaReference wrapper
     */
    @SuppressWarnings("unchecked")
    public static <V> MorphiaReference<V> wrap(V value) {
        if (value instanceof List) {
            return (MorphiaReference<V>) new ListReference<>((List<V>) value);
        } else if (value instanceof Set) {
            return (MorphiaReference<V>) new SetReference<>((Set<V>) value);
        } else if (value instanceof Map) {
            return (MorphiaReference<V>) new MapReference<>((Map<Object, V>) value);
        } else {
            return new SingleReference<>(value);
        }
    }

    static Object wrapId(Mapper mapper, FieldModel field, Object entity) {
        Object id = mapper.getId(entity);
        mapper.getEntityModel(entity.getClass());
        Object encoded = id;
        if (!entity.getClass().equals(field.getType())) {
            encoded = new DBRef(mapper.getEntityModel(entity.getClass()).getCollectionName(), encoded);
        }

        return encoded;
    }

    /**
     * @param mapper            the mapper
     * @param value             the value
     * @param optionalExtraInfo the MappedField
     * @return the encoded vale
     * @morphia.internal
     */
    public abstract Object encode(Mapper mapper, Object value, FieldModel optionalExtraInfo);

    /**
     * @return returns the referenced entity if it exists.  May return null.
     */
    public abstract T get();

    /**
     * @return the referenced IDs
     */
    public abstract List<Object> getIds();

    /**
     * @return the referenced type
     */
    public abstract Class<T> getType();

    @Override
    public int hashCode() {
        return (isResolved() ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MorphiaReference)) {
            return false;
        }

        final MorphiaReference<?> that = (MorphiaReference<?>) o;

        return getIds().equals(that.getIds());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "<<", ">>")
                   .add(getIds().toString())
                   .toString();
    }

    /**
     * Instructs Morphia to ignore missing referenced entities.  The default is to throw an exception on missing entities.
     *
     * @param ignoreMissing ignore any missing referenced documents
     * @return this
     */
    public MorphiaReference ignoreMissing(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
        return this;
    }

    /**
     * @return true if Morphia will ignore missing referenced entities.
     */
    public boolean ignoreMissing() {
        return ignoreMissing;
    }

    /**
     * @return true if this reference has already been resolved
     * @morphia.internal
     */
    public final boolean isResolved() {
        return resolved;
    }

    protected void resolve() {
        resolved = true;
    }

    /**
     * @return the datastore
     * @morphia.internal
     */
    Datastore getDatastore() {
        return datastore;
    }

    abstract Object getId(Mapper mapper, Datastore datastore, EntityModel entityModel);
}
