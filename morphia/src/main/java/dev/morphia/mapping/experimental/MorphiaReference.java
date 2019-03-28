package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper type for references to entities in other collections
 *
 * @param <T>
 * @since 1.5
 */
public abstract class MorphiaReference<T> {
    private Datastore datastore;
    private MappedClass mappedClass;

    MorphiaReference() {
    }

    MorphiaReference(final Datastore datastore, final MappedClass mappedClass) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
    }

    static Object wrapId(final Mapper mapper, final MappedField field, final Object entity) {
        Object id = mapper.getId(entity);
        Object encoded = mapper.toMongoObject(field, mapper.getMappedClass(entity), id);
        if(!entity.getClass().equals(field.getType())) {
            encoded = new DBRef(mapper.getCollectionName(entity), encoded);
        }

        return encoded;
    }

    public abstract T get();

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
    Datastore getDatastore() {
        return datastore;
    }

    /**
     * @morphia.internal
     */
    MappedClass getMappedClass() {
        return mappedClass;
    }

     @SuppressWarnings("unchecked")
     public static <V> MorphiaReference<V> wrap(final V value) {
         if(value instanceof List) {
             return (MorphiaReference<V>) new ListReference<V>((List<V>) value);
         } else if(value instanceof Set) {
             return (MorphiaReference<V>) new SetReference<V>((Set<V>)value);
         } else if(value instanceof Map) {
             return (MorphiaReference<V>) new MapReference<V>((Map<String, V>)value);
         } else {
             return new SingleReference<V>(value);
         }
     }
}
