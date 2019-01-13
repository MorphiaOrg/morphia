package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;

import static xyz.morphia.query.internal.MorphiaCursor.*;

/**
 * @morphia.internal
 * @param <T>
 */
public class MorphiaReferenceList<T> extends MorphiaReference<List<T>> {
    private List<Object> ids;
    private List<T> values;

    /**
     * @morphia.internal
     * @param datastore
     * @param mappedClass
     * @param ids
     */
    public MorphiaReferenceList(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore, mappedClass);
        this.ids = ids;
    }

    protected MorphiaReferenceList(final List<T> values, final String collection) {
        super(collection);
        set(values);
    }

    @SuppressWarnings("unchecked")
    public List<T> get() {
        if (values == null && ids != null) {
            values = toList((MongoCursor<T>) getDatastore().find(getMappedClass().getClazz())
                                                      .filter("_id in", ids)
                                                      .find());
        }
        return values;
    }

    public void set(List<T> values) {
        this.values = values;
    }

    public boolean isResolved() {
        return values != null;
    }

    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField field) {
        if(isResolved()) {
            final Class type = field.getTypeParameters().get(0).getSubClass();
            final List wrapped = get();
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
