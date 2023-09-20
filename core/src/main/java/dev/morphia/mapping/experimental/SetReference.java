package dev.morphia.mapping.experimental;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * @param <T>
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
@Deprecated(forRemoval = true, since = "2.3")
public class SetReference<T> extends CollectionReference<Set<T>> {
    private Set<T> values;

    /**
     * @param datastore   the datastore to use
     * @param entityModel the entity's mapped class
     * @param ids         the IDs
     * @morphia.internal
     */
    @MorphiaInternal
    public SetReference(DatastoreImpl datastore, EntityModel entityModel, List ids) {
        super(datastore, entityModel, ids);
    }

    /**
     * Creates an instance with prepopulated values.
     *
     * @param values the values to use
     */
    public SetReference(DatastoreImpl datastore, Set<T> values) {
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
