package dev.morphia.mapping.codec;

import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

public class ArrayFieldAccessor extends FieldAccessor {

    private TypeData typeData;
    private Class<?> componentType;

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
            Array.set(newArray, i, convert(value[i], componentType));
        }
        return newArray;
    }

    private Object convert(final List value) {
        final Object newArray = Array.newInstance(componentType, value.size());
        for (int i = 0; i < value.size(); i++) {
            Array.set(newArray, i, convert(value.get(i), componentType));
        }
        return newArray;
    }


    private Object convert(final Object o, final Class type) {
        if(o instanceof List) {
            List list = (List) o;
            final Object newArray = Array.newInstance(type.getComponentType(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(newArray, i, convert(list.get(i), type.getComponentType()));
            }

            return newArray;
        }
        return Conversions.convert(o, type);
    }
}
