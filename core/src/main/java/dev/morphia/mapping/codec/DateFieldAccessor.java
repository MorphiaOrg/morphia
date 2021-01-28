package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

public class DateFieldAccessor implements PropertyAccessor<Object> {

    private final Field field;

    private final Constructor<? extends Date> longConstructor;

    /**
     * Creates the accessor for a {@link Date} (and subclass) field.
     *
     * @param field the field itself
     */
    public DateFieldAccessor(final Field field) {

        final Class<?> dateClass = field.getType();

        if (!Date.class.isAssignableFrom(dateClass)) {
            throw new IllegalArgumentException(Date.class + " is not assignable from " + field.getType());
        }

        this.field = field;

        try {
            this.longConstructor = (Constructor<? extends Date>) dateClass.getConstructor(long.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(dateClass + " does not have constructor matching" +
                                               dateClass.getSimpleName() + "(long)");
        }

        field.setAccessible(true);

    }

    @Override
    public Object get(final Object instance) {
        try {
            return (Date) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public void set(final Object instance, final Object value) {

        final long time = ((Date)value).getTime();

        try {
            final Date toSet = longConstructor.newInstance(time);
            field.set(instance, toSet);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new MappingException(e.getMessage(), e);
        }

    }

}
