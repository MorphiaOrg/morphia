package dev.morphia.converters;


import com.mongodb.DBObject;
import dev.morphia.ObjectFactory;
import dev.morphia.mapping.EphemeralMappedField;
import dev.morphia.mapping.MappedField;
import dev.morphia.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class IterableConverter extends TypeConverter {
    @Override
    @SuppressWarnings("unchecked")
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField mf) {
        if (mf == null || fromDBObject == null) {
            return fromDBObject;
        }

        final Class subtypeDest = mf.getSubClass();
        final Collection values = createNewCollection(mf);

        final Converters converters = getMapper().getConverters();
        if (fromDBObject.getClass().isArray()) {
            //This should never happen. The driver always returns list/arrays as a List
            for (final Object o : (Object[]) fromDBObject) {
                values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
            }
        } else if (fromDBObject instanceof Iterable) {
            // map back to the java data type
            // (List/Set/Array[])
            for (final Object o : (Iterable) fromDBObject) {
                if (o instanceof DBObject) {
                    final List<MappedField> typeParameters = mf.getTypeParameters();
                    if (!typeParameters.isEmpty()) {
                        final MappedField mappedField = typeParameters.get(0);
                        if (mappedField instanceof EphemeralMappedField) {
                            values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mappedField));
                        } else {
                            throw new UnsupportedOperationException("mappedField isn't an EphemeralMappedField");
                        }
                    } else {
                        values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
                    }
                } else {
                    values.add(converters.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, mf));
                }
            }
        } else {
            //Single value case.
            values.add(converters.decode((subtypeDest != null) ? subtypeDest : fromDBObject.getClass(), fromDBObject, mf));
        }

        //convert to and array if that is the destination type (not a list/set)
        if (mf.getType().isArray()) {
            return ReflectionUtils.convertToArray(subtypeDest, (List) values);
        } else {
            return values;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object encode(final Object value, final MappedField mf) {

        if (value == null) {
            return null;
        }

        final Iterable<?> iterableValues;

        if (value.getClass().isArray()) {
            if (Array.getLength(value) == 0 || value.getClass().getComponentType().isPrimitive()) {
                return value;
            }

            iterableValues = Arrays.asList((Object[]) value);
        } else {
            if (!(value instanceof Iterable)) {
                throw new ConverterException(format("Cannot cast %s to Iterable for MappedField: %s", value.getClass(), mf));
            }

            // cast value to a common interface
            iterableValues = (Iterable<?>) value;
        }

        final List values = new ArrayList();
        if (mf != null && mf.getSubClass() != null) {
            for (final Object o : iterableValues) {
                values.add(getMapper().getConverters().encode(mf.getSubClass(), o));
            }
        } else {
            for (final Object o : iterableValues) {
                values.add(getMapper().getConverters().encode(o));
            }
        }
        return !values.isEmpty() || getMapper().getOptions().isStoreEmpties() ? values : null;
    }

    @Override
    protected boolean isSupported(final Class c, final MappedField mf) {
        if (mf != null) {
            return mf.isMultipleValues() && !mf.isMap(); //&& !mf.isTypeMongoCompatible();
        } else {
            return c.isArray() || ReflectionUtils.implementsInterface(c, Iterable.class);
        }
    }

    private Collection<?> createNewCollection(final MappedField mf) {
        final ObjectFactory of = getMapper().getOptions().getObjectFactory();
        return mf.isSet() ? of.createSet(mf) : of.createList(mf);
    }
}
