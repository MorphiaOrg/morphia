package org.mongodb.morphia.converters;


import com.mongodb.DBObject;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class IterableConverter extends TypeConverter {
    private final DefaultConverters chain;

    public IterableConverter(final DefaultConverters chain) {
        this.chain = chain;
    }

    @Override
    protected boolean isSupported(final Class c, final MappedField mf) {
        if (mf != null) {
            return mf.isMultipleValues() && !mf.isMap(); //&& !mf.isTypeMongoCompatible();
        } else {
            return c.isArray() || ReflectionUtils.implementsInterface(c, Iterable.class);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField mf) {
        if (mf == null || fromDBObject == null) {
            return fromDBObject;
        }

        final Class subtypeDest = mf.getSubClass();
        final Collection values = createNewCollection(mf);

        if (fromDBObject.getClass().isArray()) {
            //This should never happen. The driver always returns list/arrays as a List
            for (final Object o : (Object[]) fromDBObject) {
                values.add(chain.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
            }
        } else if (fromDBObject instanceof Iterable) {
            // map back to the java data type
            // (List/Set/Array[])
            for (final Object o : (Iterable) fromDBObject) {
                if (o instanceof DBObject) {
                    final MappedField mappedField = mf.getTypeParameters().get(0);
                    final Object o1 = getMapper().fromDBObject(mappedField.getType(), (DBObject) o, new DefaultEntityCache());
                    values.add(o1);
                } else {
                    values.add(chain.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
                }
            }
        } else {
            //Single value case.
            values.add(chain.decode((subtypeDest != null) ? subtypeDest : fromDBObject.getClass(), fromDBObject, mf));
        }

        //convert to and array if that is the destination type (not a list/set)
        if (mf.getType().isArray()) {
            return ReflectionUtils.convertToArray(subtypeDest, (List) values);
        } else {
            return values;
        }
    }

    private Collection<?> createNewCollection(final MappedField mf) {
        final ObjectFactory of = getMapper().getOptions().getObjectFactory();
        return mf.isSet() ? of.createSet(mf) : of.createList(mf);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object encode(final Object value, final MappedField mf) {

        if (value == null) {
            return null;
        }

        final Iterable<?> iterableValues;

        if (value.getClass().isArray()) {

            if (Array.getLength(value) == 0) {
                return value;
            }

            if (value.getClass().getComponentType().isPrimitive()) {
                return value;
            }

            iterableValues = Arrays.asList((Object[]) value);
        } else {
            if (!(value instanceof Iterable)) {
                throw new ConverterException("Cannot cast " + value.getClass() + " to Iterable for MappedField: " + mf);
            }

            // cast value to a common interface
            iterableValues = (Iterable<?>) value;
        }

        final List values = new ArrayList();
        if (mf != null && mf.getSubClass() != null) {
            for (final Object o : iterableValues) {
                values.add(chain.encode(mf.getSubClass(), o));
            }
        } else {
            for (final Object o : iterableValues) {
                values.add(chain.encode(o));
            }
        }
        if (!values.isEmpty() || getMapper().getOptions().isStoreEmpties()) {
            return values;
        } else {
            return null;
        }
    }
}
