package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

/**
 * @morphia.internal
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class SingleReference<T> extends MorphiaReference<T> {
    private Object id;
    private T value;

    private SingleReference() {
    }

    /**
     * @morphia.internal
     */
    public SingleReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final Object id) {
        super(datastore, mappedClass, collection);
        this.id = id;
    }

    protected SingleReference(final T value, final String collection) {
        super(collection);
        set(value);
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null && id != null) {
            final MongoCursor<?> mongoCursor = buildQuery()
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
            final Object entity = get();
            Object id = wrapId(mapper, entity);
            return mapper.toMongoObject(optionalExtraInfo, mapper.getMappedClass(entity), id);
        } else {
            return null;
        }
        
    }

}
