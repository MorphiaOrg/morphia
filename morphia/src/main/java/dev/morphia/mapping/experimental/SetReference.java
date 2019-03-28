package dev.morphia.mapping.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
class SetReference<T> extends CollectionReference<Set<T>> {
    private Set<T> values;

    /**
     * @morphia.internal
     */
    SetReference(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore, mappedClass, ids);
    }

    SetReference(final Set<T> values) {
        this.values = values;
    }

    @Override
    Set<T> getValues() {
        return values;
    }

    public Set<T> get() {
        if (values == null && getIds() != null) {
            values = new LinkedHashSet(find());
        }
        return values;
    }
}
