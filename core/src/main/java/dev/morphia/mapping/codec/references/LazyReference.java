package dev.morphia.mapping.codec.references;

import java.util.List;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @param <T>
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
abstract class LazyReference<T> {
    private boolean ignoreMissing;
    private boolean resolved;
    private final MorphiaDatastore datastore;

    LazyReference(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Nullable
    public abstract T get();

    public abstract List<Object> getIds();

    public abstract Class<T> getType();

    abstract Object getId(Mapper mapper, EntityModel entityModel);

    final MorphiaDatastore getDatastore() {
        return datastore;
    }

    final Mapper getMapper() {
        return datastore.getMapper();
    }

    final LazyReference<T> ignoreMissing(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
        return this;
    }

    final boolean ignoreMissing() {
        return ignoreMissing;
    }

    final boolean isResolved() {
        return resolved;
    }

    final void resolve() {
        resolved = true;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "<<", ">>")
                .add(getIds().toString())
                .toString();
    }

    @Override
    public int hashCode() {
        return (isResolved() ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LazyReference)) {
            return false;
        }
        final LazyReference<?> that = (LazyReference<?>) o;
        return getIds().equals(that.getIds());
    }
}
