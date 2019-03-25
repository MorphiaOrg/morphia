package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.morphia.query.internal.MorphiaCursor.toSet;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class SetReference<T> extends CollectionReference<Set<T>> {
    private Set<T> values;

    /**
     * @morphia.internal
     */
    public SetReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final List ids) {
        super(datastore, mappedClass, collection, ids);
    }

    protected SetReference(final Set<T> values, final String collection) {
        super(collection);
        set(values);
    }

    @Override
    public Set<T> getValues() {
        return values;
    }

    public Set<T> get() {
        if (values == null && getIds() != null) {
            values = new LinkedHashSet(find());
        }
        return values;
    }

    public void set(Set<T> values) {
        this.values = values;
    }
}
