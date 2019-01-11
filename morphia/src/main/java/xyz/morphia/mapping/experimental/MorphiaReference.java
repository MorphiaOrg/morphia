package xyz.morphia.mapping.experimental;

import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.List;

public abstract class MorphiaReference<T> {
    public abstract T get();

    public abstract void set(T value);

    /**
     * @morphia.internal
     * @return
     */
    public abstract boolean isResolved();

    public abstract Object encode(final Mapper mapper, Object value, MappedField optionalExtraInfo);

    public static <V> MorphiaReference<V> wrap(final V value) {
        if(value instanceof List) {
            return (MorphiaReference<V>) new MorphiaReferenceList<V>((List<V>)value, null);
        } else {
            return new SingleReference<V>(value, null);
        }
    }

    public static <V> MorphiaReference<V> wrap(final V value, String collection) {
        return new SingleReference<V>(value, collection);
    }
}
