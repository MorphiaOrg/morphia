package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MorphiaReference<T> {
    private Datastore datastore;
    private MappedClass mappedClass;
    private final String collection;

    MorphiaReference(final Datastore datastore, final MappedClass mappedClass, final String collection) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.collection = collection;
    }

    public MorphiaReference(final String collection) {
        this.collection = collection;
    }

    public static Object wrapId(final Mapper mapper, final MappedField field, final String collection, final Object entity) {
        Object id = mapper.getId(entity);
        Object encoded = mapper.toMongoObject(field, mapper.getMappedClass(entity), id);
        if(!entity.getClass().equals(field.getType())) {
            encoded = new DBRef(collection != null ? collection : mapper.getCollectionName(entity), encoded);
        }

        return encoded;
    }

    public abstract T get();

    public abstract void set(T value);

    /**
     * @morphia.internal
     */
    protected String getCollection() {
        return collection;
    }

    /**
     * @morphia.internal
     */
    public abstract boolean isResolved();

    /**
     * @morphia.internal
     */
    public abstract Object encode(final Mapper mapper, Object value, MappedField optionalExtraInfo);

    /**
     * @morphia.internal
     */
    protected Datastore getDatastore() {
        return datastore;
    }

    /**
     * @morphia.internal
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

}
