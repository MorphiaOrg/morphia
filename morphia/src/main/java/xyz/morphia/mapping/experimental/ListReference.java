package xyz.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;

import static xyz.morphia.query.internal.MorphiaCursor.toList;

/**
 * @param <T>
 * @morphia.internal
 */
public class ListReference<T> extends MorphiaReference<List<T>> {
    private List<Object> ids;
    private List<T> values;

    /**
     * @morphia.internal
     */
    public ListReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final List ids) {
        super(datastore, mappedClass, collection);
        this.ids = unwrap(ids);
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

    protected ListReference(final List<T> values, final String collection) {
        super(collection);
        set(values);
    }

    public boolean isResolved() {
        return values != null;
    }

    public List<T> get() {
        if (values == null && ids != null) {
            values = toList(find());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    MongoCursor<T> find() {
        return (MongoCursor<T>) buildQuery()
                                    .filter("_id in", ids)
                                    .find();
    }

    @Override
    public void set(List<T> values) {
        this.values = values;
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
