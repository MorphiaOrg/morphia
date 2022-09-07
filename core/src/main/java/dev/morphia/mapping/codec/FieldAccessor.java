package dev.morphia.mapping.codec;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;

/**
 * @morphia.internal
 */
@MorphiaInternal
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
    @Nullable
    public Object get(@Nullable Object instance) {
        try {
            return instance != null ? field.get(instance) : null;
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
