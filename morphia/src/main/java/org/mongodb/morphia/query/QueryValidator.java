package org.mongodb.morphia.query;

import com.mongodb.DBObject;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;

//TODO: Trisha - this really needs a test, if only to document what it's doing
class QueryValidator {
    private static final Logger LOG = MorphiaLoggerFactory.get(QueryValidator.class);

    private static boolean isCompatibleForOperator(final MappedField mf, final Class<?> type, final FilterOperator op,
                                                   final Object value) {
        if (value == null || type == null) {
            return true;
        } else if (op.equals(FilterOperator.EXISTS) && (value instanceof Boolean)) {
            return true;
        } else if (op.equals(FilterOperator.SIZE) && (type.isAssignableFrom(List.class) && value instanceof Integer)) {
            return true;
        } else if (op.equals(FilterOperator.IN) && (value.getClass().isArray() || Iterable.class.isAssignableFrom(value.getClass())
                                                    || Map.class.isAssignableFrom(value.getClass()))) {
            return true;
        } else if (op.equals(FilterOperator.NOT_IN) && (value.getClass().isArray() || Iterable.class.isAssignableFrom(value.getClass())
                                                        || Map.class.isAssignableFrom(value.getClass()))) {
            return true;
        } else if (op.equals(FilterOperator.MOD) && value.getClass().isArray()) {
            return ReflectionUtils.isIntegerType(Array.get(value, 0).getClass());
        } else if (op.equals(FilterOperator.GEO_WITHIN)
                   && (type.isArray() || Iterable.class.isAssignableFrom(type))
                   && (mf.getSubType() instanceof Number || asList(int.class, long.class, double.class,
                                                                   float.class).contains(mf.getSubType()))) {
            if (value instanceof DBObject) {
                String key = ((DBObject) value).keySet().iterator().next();
                return key.equals("$box") || key.equals("$center") || key.equals("$centerSphere") || key.equals("$polygon");
            }
            return false;
        } else if (op.equals(FilterOperator.ALL)
                   && (value.getClass().isArray() || Iterable.class.isAssignableFrom(value.getClass())
                       || Map.class.isAssignableFrom(value.getClass()))) {
            return true;
        } else if (value instanceof Integer && (asList(int.class, long.class, Long.class).contains(type))) {
            return true;
        } else if ((value instanceof Integer || value instanceof Long) && (asList(double.class, Double.class).contains(type))) {
            return true;
        } else if (value instanceof Pattern && String.class.equals(type)) {
            return true;
        } else if (value.getClass().getAnnotation(Entity.class) != null && Key.class.equals(type)) {
            return true;
        } else if (value.getClass().isAssignableFrom(Key.class) && type.equals(((Key) value).getKindClass())) {
            return true;
        } else if (value instanceof List<?>) {
            return true;
        } else if (mf.getMapper().getMappedClass(type) != null && mf.getMapper().getMappedClass(type).getMappedIdField() != null
                   && value.getClass().equals(mf.getMapper().getMappedClass(type).getMappedIdField().getConcreteType())) {
            return true;
        } else if (!value.getClass().isAssignableFrom(type) && !value.getClass()
                                                                     .getSimpleName()
                                                                     .equalsIgnoreCase(type.getSimpleName())) {
            return false;
        }
        return true;
    }

    /**
     * Validate the path, and value type, returning the mapped field for the field at the path
     */
    static MappedField validateQuery(final Class clazz, final Mapper mapper, final StringBuilder origProp, final FilterOperator op,
                                     final Object val, final boolean validateNames, final boolean validateTypes) {
        //TODO: cache validations (in static?).

        MappedField mf = null;
        final String prop = origProp.toString();
        boolean hasTranslations = false;

        if (validateNames) {
            final String[] parts = prop.split("\\.");
            if (clazz == null) {
                return null;
            }

            MappedClass mc = mapper.getMappedClass(clazz);
            //CHECKSTYLE:OFF
            for (int i = 0; ; ) {
                //CHECKSTYLE:ON
                final String part = parts[i];
                mf = mc.getMappedField(part);

                //translate from java field name to stored field name
                if (mf == null) {
                    mf = mc.getMappedFieldByJavaField(part);
                    if (mf == null) {
                        throw new ValidationException(format("The field '%s' could not be found in '%s' while validating - %s; if "
                                                             + "you wish to continue please disable validation.", part,
                                                             clazz.getName(), prop
                                                            ));
                    }
                    hasTranslations = true;
                    parts[i] = mf.getNameToStore();
                }

                i++;
                if (mf.isMap()) {
                    //skip the map key validation, and move to the next part
                    i++;
                }

                //catch people trying to search/update into @Reference/@Serialized fields
                if (i < parts.length && !canQueryPast(mf)) {
                    throw new ValidationException(format("Can not use dot-notation past '%s' could not be found in '%s' while"
                                                         + " validating - %s", part, clazz.getName(), prop));
                }

                if (i >= parts.length) {
                    break;
                }
                //get the next MappedClass for the next field validation
                mc = mapper.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubClass());
            }

            //record new property string if there has been a translation to any part
            if (hasTranslations) {
                origProp.setLength(0); // clear existing content
                origProp.append(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    origProp.append('.');
                    origProp.append(parts[i]);
                }
            }

            if (validateTypes) {
                boolean compatibleForType = isCompatibleForOperator(mf, mf.getType(), op, val);
                boolean compatibleForSubclass = isCompatibleForOperator(mf, mf.getSubClass(), op, val);

                if ((mf.isSingleValue() && !compatibleForType)
                    || mf.isMultipleValues() && !(compatibleForSubclass || compatibleForType)) {

                    if (LOG.isWarningEnabled()) {
                        LOG.warning(format("The type(s) for the query/update may be inconsistent; using an instance of type '%s' "
                                           + "for the field '%s.%s' which is declared as '%s'", val.getClass().getName(),
                                           mf.getDeclaringClass().getName(), mf.getJavaFieldName(), mf.getType().getName()
                                          ));
                    }
                }
            }
        }
        return mf;
    }

    private static boolean canQueryPast(final MappedField mf) {
        return !(mf.hasAnnotation(Reference.class) || mf.hasAnnotation(Serialized.class));
    }

}
