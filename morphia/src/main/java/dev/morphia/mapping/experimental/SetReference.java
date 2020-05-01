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
public class SetReference<T> extends CollectionReference<Set<T>> {
    private Set<T> values;

    /**
     * @param datastore   the datastore to use
     * @param mappedClass the entity's mapped class
     * @param ids         the IDs
     * @morphia.internal
     */
    public SetReference(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore, mappedClass, ids);
    }

    SetReference(final Set<T> values) {
        this.values = values;
    }

    @Override
    Set<T> getValues() {
        return values;
    }

    @Override
    protected void setValues(final List ids) {
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
