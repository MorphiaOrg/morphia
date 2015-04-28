package org.mongodb.morphia.converters;

import com.mongodb.DBObject;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import org.mongodb.morphia.mapping.MappingException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public abstract class Converters {
    private static final Logger LOG = MorphiaLoggerFactory.get(Converters.class);

    private final Mapper mapper;
    private final List<TypeConverter> untypedTypeEncoders = new LinkedList<TypeConverter>();
    private final Map<Class, List<TypeConverter>> tcMap = new ConcurrentHashMap<Class, List<TypeConverter>>();
    private final List<Class<? extends TypeConverter>> registeredConverterClasses = new ArrayList<Class<? extends TypeConverter>>();

    public Converters(final Mapper mapper) {
        this.mapper = mapper;
    }

    public TypeConverter addConverter(final Class<? extends TypeConverter> clazz) {
        return addConverter(mapper.getOptions().getObjectFactory().createInstance(clazz));
    }

    /**
     * Add a type converter. If it is a duplicate for an existing type, it will override that type.
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

    private void addTypedConverter(final Class type, final TypeConverter tc) {
        if (tcMap.containsKey(type)) {
            tcMap.get(type).add(0, tc);
            LOG.warning("Added duplicate converter for " + type + " ; " + tcMap.get(type));
        } else {
            final List<TypeConverter> values = new ArrayList<TypeConverter>();
            values.add(tc);
            tcMap.put(type, values);
        }
    }

    public Object decode(final Class c, final Object fromDBObject, final MappedField mf) {
        Class toDecode = c;
        if (toDecode == null) {
            toDecode = fromDBObject.getClass();
        }
        return getEncoder(toDecode).decode(toDecode, fromDBObject, mf);
    }

    protected TypeConverter getEncoder(final Class c) {
        final List<TypeConverter> tcs = tcMap.get(c);
        if (tcs != null) {
            if (tcs.size() > 1) {
                LOG.warning("Duplicate converter for " + c + ", returning first one from " + tcs);
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

    public Object encode(final Object o) {
        if (o == null) {
            return null;
        }
        return encode(o.getClass(), o);
    }

    public Object encode(final Class c, final Object o) {
        return getEncoder(c).encode(o);
    }

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

    private TypeConverter getEncoder(final MappedField mf) {
        return getEncoder(null, mf);
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
                LOG.warning("Duplicate converter for " + mf.getType() + ", returning first one from " + tcs);
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

    public boolean hasDbObjectConverter(final MappedField c) {
        final TypeConverter converter = getEncoder(c);
        return converter != null && !(converter instanceof IdentityConverter) && !(converter instanceof SimpleValueConverter);
    }

    public boolean hasDbObjectConverter(final Class c) {
        final TypeConverter converter = getEncoder(c);
        return converter != null && !(converter instanceof IdentityConverter) && !(converter instanceof SimpleValueConverter);
    }

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

    public boolean hasSimpleValueConverter(final Class c) {
        return (getEncoder(c) instanceof SimpleValueConverter);
    }

    public boolean hasSimpleValueConverter(final MappedField c) {
        return (getEncoder(c) instanceof SimpleValueConverter);
    }

    public boolean isRegistered(final Class<? extends TypeConverter> tcClass) {
        return registeredConverterClasses.contains(tcClass);
    }

    /**
     * Removes the type converter.
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

    public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj, final MapperOptions opts) {
        final Object fieldValue = mf.getFieldValue(containingObject);
        final TypeConverter enc = getEncoder(fieldValue, mf);

        final Object encoded = enc.encode(fieldValue, mf);
        if (encoded != null || opts.isStoreNulls()) {
            dbObj.put(mf.getNameToStore(), encoded);
        }
    }
}
