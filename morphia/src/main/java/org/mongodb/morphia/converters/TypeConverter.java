package org.mongodb.morphia.converters;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

import java.util.Arrays;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class TypeConverter {
    private Mapper mapper;
    private Class[] supportedTypes;

    protected TypeConverter() {
    }

    protected TypeConverter(final Class... types) {
        supportedTypes = copy(types);
    }

    public Mapper getMapper() {
        return mapper;
    }

    Class[] copy(final Class[] array) {
        return array == null ? null : Arrays.copyOf(array, array.length);
    }

    /**
     * @deprecated use #getSupportedTypes()
     */
    @Deprecated
    public Class[] getSupportTypes() {
        return copy(supportedTypes);
    }

    /**
     * returns list of supported convertable types
     */
    final Class[] getSupportedTypes() {
        return copy(supportedTypes);
    }

    public void setSupportedTypes(final Class[] supportedTypes) {
        this.supportedTypes = copy(supportedTypes);
    }

    /**
     * @deprecated use #setSupportedTypes(Class[])
     */
    @Deprecated
    public void setSupportTypes(final Class[] supportTypes) {
        this.supportedTypes = copy(supportTypes);
    }

    /**
     * checks if the class is supported for this converter.
     */
    final boolean canHandle(final Class c) {
        return isSupported(c, null);
    }

    /**
     * checks if the class is supported for this converter.
     */
    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        return false;
    }

    /**
     * checks if the MappedField is supported for this converter.
     */
    final boolean canHandle(final MappedField mf) {
        return isSupported(mf.getType(), mf);
    }

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object
     * <br><b>NOTE: optionalExtraInfo might be null</b>
     *
     */
    public abstract Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo);

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object
     */
    public final Object decode(final Class targetClass, final Object fromDBObject) {
        return decode(targetClass, fromDBObject, null);
    }

    /**
     * encode the type safe java object into the corresponding {@link com.mongodb.DBObject}
     */
    public final Object encode(final Object value) {
        return encode(value, null);
    }

    /**
     * checks if Class f is in classes *
     */
    protected boolean oneOf(final Class f, final Class... classes) {
        return oneOfClasses(f, classes);
    }

    /**
     * checks if Class f is in classes *
     */
    protected boolean oneOfClasses(final Class f, final Class[] classes) {
        for (final Class c : classes) {
            if (c.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * encode the (type-safe) java object into the corresponding {@link com.mongodb.DBObject}
     */
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return value; // as a default impl
    }

    public void setMapper(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}
