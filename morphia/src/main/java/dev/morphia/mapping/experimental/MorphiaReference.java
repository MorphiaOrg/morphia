package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.annotations.Handler;
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
@SuppressWarnings("unchecked")
@Handler(MorphiaReferenceCodec.class)
public abstract class MorphiaReference<T> {
    private Datastore datastore;

    MorphiaReference() {
    }

    MorphiaReference(final Datastore datastore) {
        this.datastore = datastore;
    }

    static Object wrapId(final Mapper mapper, final MappedField field, final Object entity) {
        Object id = mapper.getId(entity);
        mapper.getMappedClass(entity.getClass());
        Object encoded = id;
        if (!entity.getClass().equals(field.getType())) {
            encoded = new DBRef(mapper.getMappedClass(entity.getClass()).getCollectionName(), encoded);
        }

        return encoded;
    }

    /**
     * @return returns the referenced entity if it exists.  May return null.
     */
    public abstract T get();

    abstract Object getId(final Mapper mapper, final MappedClass mappedClass);

    /**
     * @return true if this reference has already been resolved
     * @morphia.internal
     */
    public abstract boolean isResolved();

    /**
     * @param mapper            the mapper
     * @param value             the value
     * @param optionalExtraInfo the MappedField
     * @return the encoded vale
     * @morphia.internal
     */
    public abstract Object encode(final Mapper mapper, Object value, MappedField optionalExtraInfo);

    /**
     * @return the datastore
     * @morphia.internal
     */
    Datastore getDatastore() {
        return datastore;
    }

    /**
     * Wraps an value in a MorphiaReference to storing on an entity
     * @param value the value wrap
     * @param <V> the type of the value
     * @return the MorphiaReference wrapper
     */
    @SuppressWarnings("unchecked")
    public static <V> MorphiaReference<V> wrap(final V value) {
        if (value instanceof List) {
            return (MorphiaReference<V>) new ListReference<>((List<V>) value);
        } else if (value instanceof Set) {
            return (MorphiaReference<V>) new SetReference<>((Set<V>) value);
        } else if (value instanceof Map) {
            return (MorphiaReference<V>) new MapReference<>((Map<String, V>) value);
        } else {
            return new SingleReference<>(value);
        }
    }
}
