package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.query.Query;

import java.util.List;

public class SingleReference<T> extends MorphiaReference<T> {
    private final String collection;
    private Object id;
    private T value;
    private Datastore datastore;
    private MappedClass mappedClass;

    private SingleReference() {
        collection = null;
    }

    /**
     * @morphia.internal
     * @param datastore
     * @param mappedClass
     * @param id
     */
    public SingleReference(final Datastore datastore, final MappedClass mappedClass, final Object id) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.id = id;
        collection = null;
    }

    protected SingleReference(final T value, final String collection) {
        this.collection = collection;
        set(value);
    }

    /**
     * @morphia.internal
     */
    public static <V> SingleReference<V> wrapId(final V id) {
        final SingleReference<V> reference = new SingleReference<V>();
        reference.id = id;
        return reference;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null && id != null) {
            final Query<?> query = datastore.find(mappedClass.getClazz())
                                            .filter("_id", id);
            final MongoCursor<?> mongoCursor = query.find();
            value = (T) mongoCursor.tryNext();
        }
        return value;
    }

    public void set(T value) {
        if(datastore != null) {
            id = datastore.getMapper().getId(value);
        }
        this.value = value;
    }

    public Object getId() {
        return id;
    }

    /**
     * @morphia.internal
     * @return
     */
    public boolean isResolved() {
        return value != null;
    }

    @Override
    public Object encode(Mapper mapper, final Object value, final MappedField optionalExtraInfo) {
        if(isResolved()) {
            final Object wrapped = get();
            Object id = getId();
            if(id == null) {
                id = mapper.getId(wrapped);
            }
            return mapper.toMongoObject(optionalExtraInfo, mapper.getMappedClass(wrapped), id);
        } else {
            return null;
        }
        
    }
}
