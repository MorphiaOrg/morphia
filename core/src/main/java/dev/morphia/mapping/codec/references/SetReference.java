package dev.morphia.mapping.codec.references;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @param <T>
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
class SetReference<T> extends CollectionReference<Set<T>> {
    private Set<T> values;

    SetReference(MorphiaDatastore datastore, EntityModel entityModel, List ids) {
        super(datastore, entityModel, ids);
    }

    SetReference(MorphiaDatastore datastore, Set<T> values) {
        super(datastore);
        this.values = values;
    }

    @Override
    Set<T> getValues() {
        return values;
    }

    @Override
    protected void setValues(List ids) {
        values = new LinkedHashSet<>();
        values.addAll(ids);
        resolve();
    }

    @Override
    public Set<T> get() {
        if (values == null) {
            values = new LinkedHashSet<T>(find());
        }
        return values;
    }
}
