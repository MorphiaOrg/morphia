package dev.morphia.mapping.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>
 * @morphia.internal
 */
public class ListReference<T> extends CollectionReference<List<T>> {
    private List<T> values;

    /**
     * @param datastore   the datastore to use
     * @param ids         the IDs of the entities
     * @param mappedClass the MappedClass for the entity type
     * @morphia.internal
     */
    public ListReference(Datastore datastore, MappedClass mappedClass, List ids) {
        super(datastore, mappedClass, ids);
    }

    ListReference(List<T> values) {
        this.values = values;
    }

    @Override
    List<?> getValues() {
        return values;
    }

    @Override
    protected void setValues(List ids) {
        values = new ArrayList<>();
        values.addAll(ids);
        resolve();
    }


    @Override
    public List<T> get() {
        if (values == null) {
            values = (List<T>) find();
        }
        return values;
    }
}
