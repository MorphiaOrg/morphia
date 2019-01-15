package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static xyz.morphia.query.internal.MorphiaCursor.toSet;

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
        this.ids = ids;
    }

    protected SetReference(final Set<T> values, final String collection) {
        super(collection);
        set(values);
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
