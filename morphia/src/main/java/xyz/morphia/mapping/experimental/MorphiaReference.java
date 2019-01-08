package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.query.Query;

public class MorphiaReference<T> {
    private final String collection;
    private Object id;
    private T value;
    private Datastore datastore;
    private MappedClass mappedClass;

    private MorphiaReference() {
        collection = null;
    }

    protected MorphiaReference(final T value, final String collection) {
        this.collection = collection;
        set(value);
    }

    public static <V> MorphiaReference<V> wrap(final V value) {
        return new MorphiaReference<V>(value, null);
    }

    public static <V> MorphiaReference<V> wrap(final V value, String collection) {
        return new MorphiaReference<V>(value, collection);
    }

    /**
     * @morphia.internal
     */
    public static <V> MorphiaReference<V> wrapId(final V id) {
        final MorphiaReference<V> reference = new MorphiaReference<V>();
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
        return value != null;
    }
}
