package xyz.morphia.mapping.experimental;

import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MorphiaReference<T> {
    private String collection;
    private Datastore datastore;
    private MappedClass mappedClass;

    MorphiaReference() {
    }

    MorphiaReference(final String collection) {
        this.collection = collection;
    }

    MorphiaReference(final Datastore datastore, final MappedClass mappedClass) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
    }

    public abstract T get();

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
        return wrap(value, null);
    }

    @SuppressWarnings("unchecked")
    public static <V> MorphiaReference<V> wrap(final V value, String collection) {
        if(value instanceof List) {
            return (MorphiaReference<V>) new MorphiaReferenceList<V>((List<V>) value, collection);
        } else if(value instanceof Set) {
            return (MorphiaReference<V>) new MorphiaReferenceSet<V>((Set<V>)value, collection);
        } else if(value instanceof Map) {
            return (MorphiaReference<V>) new MorphiaReferenceMap<V>((Map<String, V>)value, collection);
        } else {
            return new SingleReference<V>(value, collection);
        }
    }
}
