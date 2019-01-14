package xyz.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import xyz.morphia.AdvancedDatastore;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.query.Query;

/**
 * @morphia.internal
 * @param <T>
 */
public class SingleReference<T> extends MorphiaReference<T> {
    private Object id;
    private T value;

    private SingleReference() {

    }

    /**
     * @morphia.internal
     * @param datastore
     * @param mappedClass
     * @param id
     */
    public SingleReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final Object id) {
        super(datastore, mappedClass, collection);
        this.id = id;
    }

    protected SingleReference(final T value, final String collection) {
        super(collection);
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
            final Query<?> query;
            if (getCollection() == null) {
                query = getDatastore().find(getMappedClass().getClazz());
            } else {
                query = ((AdvancedDatastore) getDatastore()).find(getCollection(), getMappedClass().getClazz());
            }

            final MongoCursor<?> mongoCursor = query
                                                   .filter("_id", id)
                                                   .find();
            value = (T) mongoCursor.tryNext();
        }
        return value;
    }

    public void set(T value) {
        if(getDatastore() != null) {
            id = getDatastore().getMapper().getId(value);
        }
        this.value = value;
    }

    public Object getId() {
        return id;
    }

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
            if(getCollection() != null) {
                id = new DBRef(getCollection(), id);
            }
            return mapper.toMongoObject(optionalExtraInfo, mapper.getMappedClass(wrapped), id);
        } else {
            return null;
        }
        
    }
}
