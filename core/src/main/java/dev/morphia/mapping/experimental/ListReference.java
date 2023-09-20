package dev.morphia.mapping.experimental;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @param <T>
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@Deprecated(forRemoval = true, since = "2.3")
public class ListReference<T> extends CollectionReference<List<T>> {
    private List<T> values;

    /**
     * @param datastore
     * @param model     the EntityModel for the entity type
     * @param ids       the IDs of the entities
     * @morphia.internal
     */
    @MorphiaInternal
    public ListReference(DatastoreImpl datastore, EntityModel model, List ids) {
        super(datastore, model, ids);
    }

    /**
     * Creates an instance with prepopulated values.
     *
     * @param values the values to use
     */
    public ListReference(DatastoreImpl datastore, List<T> values) {
        super(datastore);
        this.values = values;
    }

    @Override
    List<?> getValues() {
        return values;
    }

    @Override
    protected void setValues(List ids) {
        values = new ArrayList<T>();
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
