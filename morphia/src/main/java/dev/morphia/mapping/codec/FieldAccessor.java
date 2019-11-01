package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;

/**
 * @morphia.internal
 */
public class FieldAccessor implements PropertyAccessor {
    private final Field field;

    /**
     * Creates the accessor for a field
     *
     * @param field the field itself
     */
    public FieldAccessor(final Field field) {
        this.field = field;
    }

    protected Field getField() {
        return field;
    }

    @Override
    public Object get(final Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void set(final Object instance, final Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalArgumentException e) {
            throw new MappingException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
