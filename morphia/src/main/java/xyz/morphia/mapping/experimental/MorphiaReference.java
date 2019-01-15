package xyz.morphia.mapping.experimental;

import com.mongodb.DBRef;
import xyz.morphia.AdvancedDatastore;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MorphiaReference<T> {
    private String collection;
    private Datastore datastore;
    private MappedClass mappedClass;

    protected MorphiaReference() {
    }

    MorphiaReference(final Datastore datastore, final MappedClass mappedClass, final String collection) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.collection = collection;
    }

    MorphiaReference(final String collection) {
        this.collection = collection;
    }

    MorphiaReference(final Datastore datastore, final MappedClass mappedClass) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
    }

    public abstract T get();

    protected Query<?> buildQuery() {
        final Query<?> query;
        if (getCollection() == null) {
            query = getDatastore().find(getMappedClass().getClazz());
        } else {
            query = ((AdvancedDatastore) getDatastore()).find(getCollection(), getMappedClass().getClazz());
        }
        return query;
    }

    public abstract void set(T value);

    /**
     * @morphia.internal
     * @return
     */
    public abstract boolean isResolved();

    /**
     * @morphia.internal
     * @return
     */
    public abstract Object encode(final Mapper mapper, Object value, MappedField optionalExtraInfo);

    /**
     * @morphia.internal
     * @return
     */
    protected String getCollection() {
        return collection;
    }

    /**
     * @morphia.internal
     * @return
     */
    protected Datastore getDatastore() {
        return datastore;
    }

    /**
     * @morphia.internal
     * @return
     */
    protected MappedClass getMappedClass() {
        return mappedClass;
    }

    public static <V> MorphiaReference<V> wrap(final V value) {
        return wrap(null, value);
    }

    @SuppressWarnings("unchecked")
    public static <V> MorphiaReference<V> wrap(String collection, final V value) {
        if(value instanceof List) {
            return (MorphiaReference<V>) new ListReference<V>((List<V>) value, collection);
        } else if(value instanceof Set) {
            return (MorphiaReference<V>) new SetReference<V>((Set<V>)value, collection);
        } else if(value instanceof Map) {
            return (MorphiaReference<V>) new MapReference<V>((Map<String, V>)value, collection);
        } else {
            return new SingleReference<V>(value, collection);
        }
    }

    protected Object wrapId(final Mapper mapper, final Object wrapped) {
        Object id = mapper.getId(wrapped);
        if(getCollection() != null) {
            id = new DBRef(getCollection(), id);
        }
        return id;
    }
}
