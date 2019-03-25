package dev.morphia.mapping.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;

import java.util.Collection;
import java.util.List;

import static dev.morphia.query.internal.MorphiaCursor.toList;

/**
 * @param <T>
 * @morphia.internal
 */
public class ListReference<T> extends CollectionReference<List<T>> {
    private List<T> values;

    /**
     * @morphia.internal
     */
    public ListReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final List ids) {
        super(datastore, mappedClass, collection, ids);
    }

    protected ListReference(final List<T> values, final String collection) {
        super(collection);
        set(values);
    }

    @Override
    protected Collection<?> getValues() {
        return values;
    }

    @Override
    public List<T> get() {
        if (values == null && getIds() != null) {
            values = (List<T>) find();
        }
        return values;
    }

    @Override
    public void set(List<T> values) {
        this.values = values;
    }
}
