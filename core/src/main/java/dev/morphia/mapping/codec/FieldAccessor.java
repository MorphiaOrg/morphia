package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;

/**
 * @morphia.internal
 */
public class FieldAccessor implements PropertyAccessor<Object> {
    private final Field field;

    /**
     * Creates the accessor for a field
     *
     * @param field the field itself
     */
    public FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    protected Field getField() {
        return field;
    }

    @Override
    public Object get(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            field.set(instance, value);
//            field.set(instance, Conversions.convert(value, field.getType()));
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
