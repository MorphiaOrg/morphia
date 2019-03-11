package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.morphia.query.internal.MorphiaCursor.toSet;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class SetReference<T> extends MorphiaReference<Set<T>> {
    private List<Object> ids;
    private Set<T> values;

    /**
     * @morphia.internal
     */
    public SetReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final List ids) {
        super(datastore, mappedClass, collection);
        this.ids = unwrap(ids);
    }

    protected SetReference(final Set<T> values, final String collection) {
        super(collection);
        set(values);
    }

    private List<Object> unwrap(final List ids) {
        List<Object> unwrapped = null;
        if(ids != null && !ids.isEmpty()) {
            if(ids.get(0) instanceof DBRef) {
                unwrapped = new ArrayList<Object>();
                for (final Object id : ids) {
                    unwrapped.add(((DBRef) id).getId());
                }
            } else {
                unwrapped = ids;
            }
        }

        return unwrapped;
    }

    public Set<T> get() {
        if (values == null && ids != null) {
            values = toSet(find());
        }
        return values;
    }

    public void set(Set<T> values) {
        this.values = values;
    }

    public boolean isResolved() {
        return values != null;
    }

    @SuppressWarnings("unchecked")
    MongoCursor<T> find() {
        return (MongoCursor<T>) buildQuery()
                                    .filter("_id in", ids)
                                    .find();
    }

    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField field) {
        if (isResolved()) {
            final Class type = field.getTypeParameters().get(0).getSubClass();
            List ids = new ArrayList();
            for (final Object o : get()) {
                ids.add(wrapId(mapper, o));
            }
            return mapper.toMongoObject(field, mapper.getMappedClass(type), ids);
        } else {
            return null;
        }
    }
}
