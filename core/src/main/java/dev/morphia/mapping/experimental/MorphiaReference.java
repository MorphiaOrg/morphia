package dev.morphia.mapping.experimental;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.annotations.Handler;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * Wrapper type for references to entities in other collections
 *
 * @param <T>
 * @hidden
 * @since 1.5
 * @deprecated use @{@link dev.morphia.annotations.Reference} instead. See issue
 *             <a href="https://github.com/MorphiaOrg/morphia/issues/1864">1864</a> for more.
 */
@SuppressWarnings("unchecked")
@Handler(MorphiaReferenceCodec.class)
@Deprecated(forRemoval = true, since = "2.3")
public abstract class MorphiaReference<T> {
    private boolean ignoreMissing;
    private boolean resolved;
    private Datastore datastore;

    MorphiaReference(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * Wraps an idValue in a MorphiaReference to storing on an entity
     *
     * @param value the idValue wrap
     * @param <V>   the type of the idValue
     * @return the MorphiaReference wrapper
     */
    @SuppressWarnings("unchecked")
    public static <V> MorphiaReference<V> wrap(Datastore datastore, V value) {
        if (value instanceof List) {
            return (MorphiaReference<V>) new ListReference<>(datastore, (List<V>) value);
        } else if (value instanceof Set) {
            return (MorphiaReference<V>) new SetReference<>(datastore, (Set<V>) value);
        } else if (value instanceof Map) {
            return (MorphiaReference<V>) new MapReference<>(datastore, (Map<Object, V>) value);
        } else {
            return new SingleReference<>(datastore, value);
        }
    }

    /**
     * @return returns the referenced entity if it exists. May return null.
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
        return datastore.getMapper();
    }

    /**
     * Instructs Morphia to ignore missing referenced entities. The default is to throw an exception on missing entities.
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
    @MorphiaInternal
    public final boolean isResolved() {
        return resolved;
    }

    protected void resolve() {
        resolved = true;
    }

    abstract Object getId(Mapper mapper, EntityModel entityModel);

    private <T> T fetchEntities(Object idValue) {
        if (idValue instanceof Map) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        } else if (idValue instanceof List) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        } else if (idValue instanceof Set) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
