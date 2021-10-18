package dev.morphia.mapping.experimental;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.annotations.Handler;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

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
    private Mapper mapper;
    private boolean ignoreMissing;
    private boolean resolved;

    MorphiaReference() {
    }

    MorphiaReference(Datastore datastore, Mapper mapper) {
        this.mapper = mapper;
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

    /**
     * @return returns the referenced entity if it exists.  May return null.
     */
    @Nullable
    public abstract T get();

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "<<", ">>")
            .add(getIds().toString())
            .toString();
    }

    /**
     * @return the referenced IDs
     */
    public abstract List<Object> getIds();

    protected Datastore getDatastore() {
        return datastore;
    }

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

    protected Mapper getMapper() {
        return mapper;
    }

    /**
     * Instructs Morphia to ignore missing referenced entities.  The default is to throw an exception on missing entities.
     *
     * @param ignoreMissing ignore any missing referenced documents
     * @return this
     */
    public MorphiaReference<T> ignoreMissing(boolean ignoreMissing) {
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

    abstract Object getId(Mapper mapper, EntityModel entityModel);
}
