package dev.morphia.mapping.codec;

import morphia.org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
public class ArrayFieldAccessor extends FieldAccessor {

    private TypeData typeData;
    private Class<?> componentType;

    /**
     * Creates the accessor
     *
     * @param typeData the type data
     * @param field    the field
     */
    public ArrayFieldAccessor(final TypeData typeData, final Field field) {
        super(field);
        this.typeData = typeData;
        componentType = field.getType().getComponentType();
    }

    @Override
    public void set(final Object instance, final Object value) {
        Object newValue = value;
        if (value.getClass().getComponentType() != componentType) {
            newValue = value instanceof List ? convert((List) value) : convert((Object[]) value);
        }
        super.set(instance, newValue);
    }

    private Object convert(final Object[] value) {
        final Object newArray = Array.newInstance(componentType, value.length);
        for (int i = 0; i < value.length; i++) {
            Object convert = convert(value[i], componentType);
            Array.set(newArray, i, convert);
        }
        return newArray;
    }

    private Object convert(final List value) {
        final Object newArray = Array.newInstance(componentType, value.size());
        for (int i = 0; i < value.size(); i++) {
            Object converted = convert(value.get(i), componentType);
            try {
                Array.set(newArray, i, converted);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(format("Can't set %s with a value type of %s", getField(), converted.getClass()));
            }
        }
        return newArray;
    }


    private Object convert(final Object o, final Class type) {
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
