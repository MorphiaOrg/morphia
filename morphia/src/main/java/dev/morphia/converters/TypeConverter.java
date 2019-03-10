package dev.morphia.converters;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

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

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object
     *
     * @param targetClass  the class to create and populate
     * @param fromDBObject the DBObject to use when populating the new instance
     * @return the new instance
     */
    public final Object decode(final Class targetClass, final Object fromDBObject) {
        return decode(targetClass, fromDBObject, null);
    }

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object <br><b>NOTE: optionalExtraInfo might
     * be
     * null</b>
     *
     * @param targetClass       the class to create and populate
     * @param fromDBObject      the DBObject to use when populating the new instance
     * @param optionalExtraInfo the MappedField that contains the metadata useful for decoding
     * @return the new instance
     */
    public abstract Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo);

    /**
     * encode the type safe java object into the corresponding {@link com.mongodb.DBObject}
     *
     * @param value The object to encode
     * @return the encoded version of the object
     */
    public final Object encode(final Object value) {
        return encode(value, null);
    }

    /**
     * encode the (type-safe) java object into the corresponding {@link com.mongodb.DBObject}
     *
     * @param value             The object to encode
     * @param optionalExtraInfo the MappedField that contains the metadata useful for decoding
     * @return the encoded version of the object
     */
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return value; // as a default impl
    }

    /**
     * @return the mapper used by the converter
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Sets the Mapper to use.
     *
     * @param mapper the Mapper to use
     */
    public void setMapper(final Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @return an array of supported convertable types
     * @deprecated use #getSupportedTypes()
     */
    @Deprecated
    public Class[] getSupportTypes() {
        return copy(supportedTypes);
    }

    /**
     * @param supportTypes the types this converter supports
     * @deprecated use #setSupportedTypes(Class[])
     */
    @Deprecated
    public void setSupportTypes(final Class[] supportTypes) {
        this.supportedTypes = copy(supportTypes);
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }

    /**
     * checks if the class is supported for this converter.
     */
    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        return false;
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

    Class[] copy(final Class[] array) {
        return array == null ? null : Arrays.copyOf(array, array.length);
    }

    /**
     * @return an array of supported convertable types
     */
    final Class[] getSupportedTypes() {
        return copy(supportedTypes);
    }

    /**
     * Sets the types supported by this converter.
     *
     * @param supportedTypes the types this converter supports
     */
    public void setSupportedTypes(final Class[] supportedTypes) {
        this.supportedTypes = copy(supportedTypes);
    }

    /**
     * checks if the class is supported for this converter.
     */
    final boolean canHandle(final Class c) {
        return isSupported(c, null);
    }

    /**
     * checks if the MappedField is supported for this converter.
     */
    final boolean canHandle(final MappedField mf) {
        return isSupported(mf.getType(), mf);
    }
}
