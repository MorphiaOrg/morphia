package dev.morphia.mapping.experimental;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @param <T>
 * @morphia.internal
 */
public class ListReference<T> extends CollectionReference<List<T>> {
    private List<T> values;

    /**
     * @param datastore the datastore to use
     * @param ids       the IDs of the entities
     * @param model     the EntityModel for the entity type
     * @morphia.internal
     */
    public ListReference(Datastore datastore, EntityModel model, List ids) {
        super(datastore, model, ids);
    }

    /**
     * Creates an instance with prepopulated values.
     *
     * @param values the values to use
     */
    public ListReference(List<T> values) {
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
