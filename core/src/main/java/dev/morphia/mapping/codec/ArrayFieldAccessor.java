package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
@SuppressWarnings("rawtypes")
public class ArrayFieldAccessor extends FieldAccessor {

    private final TypeData<?> typeData;
    private final Class<?> componentType;

    /**
     * Creates the accessor
     *
     * @param typeData the type data
     * @param field    the field
     */
    public ArrayFieldAccessor(TypeData<?> typeData, Field field) {
        super(field);
        this.typeData = typeData;
        componentType = field.getType().getComponentType();
    }

    @Override
    public void set(Object instance, Object value) {
        Object newValue = value;
        if (value.getClass().getComponentType() != componentType) {
            newValue = value instanceof List ? convert((List) value) : convert((Object[]) value);
        }
        super.set(instance, newValue);
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
                    throw new IllegalArgumentException(format("Can't set %s with a value type of %s", getField(), converted.getClass()));
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
                    throw new IllegalArgumentException(format("Can't set %s with a value type of %s", getField(), converted.getClass()));
                }
            }

            return newArray;
        }
        return Conversions.convert(o, type);
    }
}
