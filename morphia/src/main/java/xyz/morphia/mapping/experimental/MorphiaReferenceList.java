package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.List;

import static xyz.morphia.query.internal.MorphiaCursor.*;

public class MorphiaReferenceList<T> extends MorphiaReference<List<T>> {
    private final String collection;
    private List<Object> ids;
    private List<T> values;
    private Datastore datastore;
    private MappedClass mappedClass;

    private MorphiaReferenceList() {
        collection = null;
    }

    /**
     * @morphia.internal
     * @param datastore
     * @param mappedClass
     * @param ids
     */
    public MorphiaReferenceList(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.ids = ids;
        collection = null;
    }

    protected MorphiaReferenceList(final List<T> values, final String collection) {
        this.collection = collection;
        set(values);
    }

    @SuppressWarnings("unchecked")
    public List<T> get() {
        if (values == null && ids != null) {
            values = toList((MongoCursor<T>) datastore.find(mappedClass.getClazz())
                                                      .filter("_id in", ids)
                                                      .find());
        }
        return values;
    }

    public void set(List<T> values) {
        this.values = values;
    }

    /**
     * @morphia.internal
     */
    public void instrument(Datastore datastore, final MappedClass mappedClass) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
    }

    /**
     * @morphia.internal
     * @return
     */
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
