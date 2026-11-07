package dev.morphia.mapping.codec;

import java.lang.reflect.Array;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.codecs.pojo.PropertyAccessor;

import static java.lang.String.format;

/**
 * Wraps a property's accessor to convert a decoded array, which the codec reads as a list or an {@code Object[]}, to the
 * property's array type before writing it.
 *
 * @hidden
 * @morphia.internal
 */
@SuppressWarnings("rawtypes")
@MorphiaInternal
public class ArrayAccessor implements PropertyAccessor<Object> {

    private final PropertyAccessor<Object> delegate;
    private final Class<?> componentType;
    private final Conversions conversions;

    /**
     * Creates the accessor
     *
     * @param delegate      the property's accessor
     * @param componentType the component type of the property's array type
     * @param conversions   the Conversions instance to use
     */
    @SuppressWarnings("unchecked")
    public ArrayAccessor(PropertyAccessor<?> delegate, Class<?> componentType, Conversions conversions) {
        this.delegate = (PropertyAccessor<Object>) delegate;
        this.componentType = componentType;
        this.conversions = conversions;
    }

    /**
     * @param type the property's type, erased
     * @return true if the property's values need converting: it's an array, other than {@code byte[]}, which BSON stores
     *         as binary
     */
    public static boolean converts(Class<?> type) {
        return type.isArray() && !type.getComponentType().equals(byte.class);
    }

    @Override
    @Nullable
    public <S> Object get(S instance) {
        return delegate.get(instance);
    }

    @Override
    public <S> void set(S instance, @Nullable Object value) {
        Object newValue = value;
        if (value != null && value.getClass().getComponentType() != componentType) {
            newValue = value instanceof List ? convert((List) value) : convert((Object[]) value);
        }
        delegate.set(instance, newValue);
    }

    private Object convert(Object[] value) {
        final Object newArray = Array.newInstance(componentType, value.length);
        for (int i = 0; i < value.length; i++) {
            Object convert = convert(value[i], componentType);
            Array.set(newArray, i, convert);
        }
        return newArray;
    }

    private Object convert(List value) {
        final Object newArray = Array.newInstance(componentType, value.size());
        for (int i = 0; i < value.size(); i++) {
            Object converted = convert(value.get(i), componentType);
            if (converted != null) {
                try {
                    Array.set(newArray, i, converted);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            format("Can't set an element of %s[] to a %s", componentType.getName(), converted.getClass()));
                }
            } else {
                throw new IllegalArgumentException(format("Can not convert '%s' to type '%s' ", value.get(i), componentType.getName()));
            }
        }
        return newArray;
    }

    @Nullable
    private Object convert(Object o, Class<?> type) {
        if (o instanceof List) {
            List list = (List) o;
            final Object newArray = Array.newInstance(type.getComponentType(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Object converted = convert(list.get(i), type.getComponentType());
                try {
                    Array.set(newArray, i, converted);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            format("Can't set an element of %s[] to a %s", type.getComponentType().getName(), converted.getClass()));
                }
            }

            return newArray;
        }
        return conversions.convert(o, type);
    }
}
