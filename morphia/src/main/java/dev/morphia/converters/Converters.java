package dev.morphia.converters;

import com.mongodb.DBObject;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Defines a bundle of converters
 *
 * @deprecated this mechanism is being replaced in 2.0
 */
@Deprecated
public abstract class Converters {
    private static final Logger LOG = LoggerFactory.getLogger(Converters.class);

    private final Mapper mapper;
    private final List<TypeConverter> untypedTypeEncoders = new LinkedList<TypeConverter>();
    private final Map<Class, List<TypeConverter>> tcMap = new ConcurrentHashMap<Class, List<TypeConverter>>();
    private final List<Class<? extends TypeConverter>> registeredConverterClasses = new ArrayList<Class<? extends TypeConverter>>();

    /**
     * Creates a bundle with a particular Mapper.
     *
     * @param mapper the Mapper to use
     */
    public Converters(final Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Adds a TypeConverter to this bundle.
     *
     * @param clazz the converter to add
     * @return the new instance
     */
    public TypeConverter addConverter(final Class<? extends TypeConverter> clazz) {
        return addConverter(mapper.getOptions().getObjectFactory().createInstance(clazz));
    }

    /**
     * Add a type converter. If it is a duplicate for an existing type, it will override that type.
     *
     * @param tc the converter to add
     * @return the TypeConverter passed in
     */
    public TypeConverter addConverter(final TypeConverter tc) {
        if (tc.getSupportedTypes() != null) {
            for (final Class c : tc.getSupportedTypes()) {
                addTypedConverter(c, tc);
            }
        } else {
            untypedTypeEncoders.add(tc);
        }

        registeredConverterClasses.add(tc.getClass());
        tc.setMapper(mapper);

        return tc;
    }

    /**
     * decode the {@link com.mongodb.DBObject} and provide the corresponding java (type-safe) object
     * <br><b>NOTE: mf might be null</b>
     *
     * @param c            the class to create and populate
     * @param fromDBObject the DBObject to use when populating the new instance
     * @param mf           the MappedField that contains the metadata useful for decoding
     * @return the new instance
     */
    public Object decode(final Class c, final Object fromDBObject, final MappedField mf) {
        Class toDecode = c;
        if (toDecode == null) {
            toDecode = fromDBObject.getClass();
        }
        return getEncoder(toDecode).decode(toDecode, fromDBObject, mf);
    }

    /**
     * encode the type safe java object into the corresponding {@link com.mongodb.DBObject}
     *
     * @param o The object to encode
     * @return the encoded version of the object
     */
    public Object encode(final Object o) {
        if (o == null) {
            return null;
        }
        return encode(o.getClass(), o);
    }

    /**
     * encode the type safe java object into the corresponding {@link com.mongodb.DBObject}
     *
     * @param c The type to use when encoding
     * @param o The object to encode
     * @return the encoded version of the object
     */
    public Object encode(final Class c, final Object o) {
        return getEncoder(c).encode(o);
    }

    /**
     * Creates an entity and populates its state based on the dbObject given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     *
     * @param dbObj        the object state to use
     * @param mf           the MappedField containing the metadata to use when decoding in to a field
     * @param targetEntity then entity to hold the state from the database
     */
    public void fromDBObject(final DBObject dbObj, final MappedField mf, final Object targetEntity) {
        final Object object = mf.getDbObjectValue(dbObj);
        if (object != null) {
            final TypeConverter enc = getEncoder(mf);
            final Object decodedValue = enc.decode(mf.getType(), object, mf);
            try {
                mf.setFieldValue(targetEntity, decodedValue);
            } catch (IllegalArgumentException e) {
                throw new MappingException(format("Error setting value from converter (%s) for %s to %s",
                                                  enc.getClass().getSimpleName(), mf.getFullName(), decodedValue), e);
            }
        }
    }

    /**
     * @param field the field to check with
     * @return true if there is a converter for the type of the field
     */
    public boolean hasDbObjectConverter(final MappedField field) {
        final TypeConverter converter = getEncoder(field);
        return converter != null && !(converter instanceof IdentityConverter) && !(converter instanceof SimpleValueConverter);
    }

    /**
     * @param c the type to check
     * @return true if there is a converter for the type
     */
    public boolean hasDbObjectConverter(final Class c) {
        final TypeConverter converter = getEncoder(c);
        return converter != null && !(converter instanceof IdentityConverter) && !(converter instanceof SimpleValueConverter);
    }

    /**
     * @param o the object/type to check
     * @return true if there is a SimpleValueConverter for the type represented by o
     * @see SimpleValueConverter
     */
    public boolean hasSimpleValueConverter(final Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Class) {
            return hasSimpleValueConverter((Class) o);
        } else if (o instanceof MappedField) {
            return hasSimpleValueConverter((MappedField) o);
        } else {
            return hasSimpleValueConverter(o.getClass());
        }
    }

    /**
     * @param c the type to check
     * @return true if there is a SimpleValueConverter for the type represented by c
     * @see SimpleValueConverter
     */
    public boolean hasSimpleValueConverter(final Class c) {
        return (getEncoder(c) instanceof SimpleValueConverter);
    }

    /**
     * @param c the type to check
     * @return true if there is a SimpleValueConverter for the type represented by c
     * @see SimpleValueConverter
     */
    public boolean hasSimpleValueConverter(final MappedField c) {
        return (getEncoder(c) instanceof SimpleValueConverter);
    }

    /**
     * @param tcClass the type to check
     * @return true if a converter of this type has been registered
     */
    public boolean isRegistered(final Class<? extends TypeConverter> tcClass) {
        return registeredConverterClasses.contains(tcClass);
    }

    /**
     * Removes the type converter.
     *
     * @param tc the converter to remove
     */
    public void removeConverter(final TypeConverter tc) {
        if (tc.getSupportedTypes() == null) {
            untypedTypeEncoders.remove(tc);
            registeredConverterClasses.remove(tc.getClass());
        } else {
            for (final Entry<Class, List<TypeConverter>> entry : tcMap.entrySet()) {
                List<TypeConverter> list = entry.getValue();
                if (list.contains(tc)) {
                    list.remove(tc);
                }
                if (list.isEmpty()) {
                    tcMap.remove(entry.getKey());
                }
            }
            registeredConverterClasses.remove(tc.getClass());
        }

    }

    /**
     * Converts an entity to a DBObject
     *
     * @param containingObject The object to convert
     * @param mf               the MappedField to extract
     * @param dbObj            the DBObject to populate
     * @param opts             the options to apply
     */
    public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj, final MapperOptions opts) {
        final Object fieldValue = mf.getFieldValue(containingObject);
        final TypeConverter enc = getEncoder(fieldValue, mf);

        final Object encoded = enc.encode(fieldValue, mf);
        if (encoded != null || opts.isStoreNulls()) {
            dbObj.put(mf.getNameToStore(), encoded);
        }
    }

    protected TypeConverter getEncoder(final Class c) {
        final List<TypeConverter> tcs = tcMap.get(c);
        if (tcs != null) {
            if (tcs.size() > 1) {
                LOG.warn("Duplicate converter for " + c + ", returning first one from " + tcs);
            }
            return tcs.get(0);
        }

        for (final TypeConverter tc : untypedTypeEncoders) {
            if (tc.canHandle(c)) {
                return tc;
            }
        }

        return null;
    }

    protected TypeConverter getEncoder(final Object val, final MappedField mf) {
        List<TypeConverter> tcs = null;

        if (val != null) {
            tcs = tcMap.get(val.getClass());
        }

        if (tcs == null || (!tcs.isEmpty() && tcs.get(0) instanceof IdentityConverter)) {
            tcs = tcMap.get(mf.getType());
        }

        if (tcs != null) {
            if (tcs.size() > 1) {
                LOG.warn("Duplicate converter for " + mf.getType() + ", returning first one from " + tcs);
            }
            return tcs.get(0);
        }

        for (final TypeConverter tc : untypedTypeEncoders) {
            if (tc.canHandle(mf) || (val != null && tc.isSupported(val.getClass(), mf))) {
                return tc;
            }
        }

        return null;
    }

    private void addTypedConverter(final Class type, final TypeConverter tc) {
        if (tcMap.containsKey(type)) {
            tcMap.get(type).add(0, tc);
            LOG.warn("Added duplicate converter for " + type + " ; " + tcMap.get(type));
        } else {
            final List<TypeConverter> values = new ArrayList<TypeConverter>();
            values.add(tc);
            tcMap.put(type, values);
        }
    }

    private TypeConverter getEncoder(final MappedField mf) {
        return getEncoder(null, mf);
    }
}
