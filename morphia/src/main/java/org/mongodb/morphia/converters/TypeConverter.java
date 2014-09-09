package org.mongodb.morphia.converters;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

import java.util.Arrays;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public abstract class TypeConverter {
    //CHECKSTYLE:OFF
    /**
     * @deprecated please use the getter/setter methods
     */
    protected Mapper mapper;
    /**
     * @deprecated please use the getter/setter methods
     */
    protected Class[] supportTypes;
    //CHECKSTYLE:ON

    protected TypeConverter() {
    }

    protected TypeConverter(final Class... types) {
        supportTypes = copy(types);
    }

    public Mapper getMapper() {
        return mapper;
    }

    public Class[] getSupportTypes() {
        return copy(supportTypes);
    }

    Class[] copy(final Class[] array) {
        return array == null ? null : Arrays.copyOf(array, array.length);
    }

    /**
     * returns list of supported convertable types
     */
    final Class[] getSupportedTypes() {
        return copy(supportTypes);
    }

    public void setSupportTypes(final Class[] supportTypes) {
        this.supportTypes = copy(supportTypes);
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
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object<br><b>NOTE: optionalExtraInfo might be
     * null</b>*
     */
    public abstract Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo);

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object
     */
    public final Object decode(final Class targetClass, final Object fromDBObject) {
        return decode(targetClass, fromDBObject, null);
    }

    /**
     * encode the type safe java object into the corresponding {@link com.mongodb.DBObject}<br><b>NOTE: optionalExtraInfo might be null</b>
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
}
