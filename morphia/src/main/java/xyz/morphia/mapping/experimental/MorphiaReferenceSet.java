package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static xyz.morphia.query.internal.MorphiaCursor.toSet;

/**
 * @morphia.internal
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class MorphiaReferenceSet<T> extends MorphiaReference<Set<T>> {
    private Set<Object> ids;
    private Set<T> values;

    /**
     * @morphia.internal
     * @param datastore
     * @param mappedClass
     * @param ids
     */
    public MorphiaReferenceSet(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore, mappedClass);
        this.ids = new HashSet(ids);
    }

    protected MorphiaReferenceSet(final Set<T> values, final String collection) {
        super(collection);
        set(values);
    }

    @SuppressWarnings("unchecked")
    public Set<T> get() {
        if (values == null && ids != null) {
            values = toSet((MongoCursor<T>) getDatastore().find(getMappedClass().getClazz())
                                                      .filter("_id in", ids)
                                                      .find());
        }
        return values;
    }

    public void set(Set<T> values) {
        this.values = values;
    }

    public boolean isResolved() {
        return values != null;
    }

    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField field) {
        if(isResolved()) {
            final Class type = field.getTypeParameters().get(0).getSubClass();
            final Set wrapped = get();
            List ids = new ArrayList(wrapped.size());
            for (final Object o : wrapped) {
                ids.add(mapper.getId(o));
            }
            return mapper.toMongoObject(field, mapper.getMappedClass(type), ids);
        } else {
            return null;
        }
    }
}
