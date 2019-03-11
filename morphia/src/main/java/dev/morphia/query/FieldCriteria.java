package dev.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.utils.ReflectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines a Criteria against a field
 */
class FieldCriteria extends AbstractCriteria {
    private static final Logger LOG = LoggerFactory.getLogger(FieldCriteria.class);

    private final String field;
    private final FilterOperator operator;
    private final Object value;
    private final boolean not;
    private final QueryImpl<?> query;

    FieldCriteria(final QueryImpl<?> query, final String field, final FilterOperator op, final Object value) {
        this(query, field, op, value, false);
    }

    @SuppressWarnings("deprecation")
    FieldCriteria(final QueryImpl<?> query, final String fieldName, final FilterOperator op, final Object value, final boolean not) {
        //validate might modify prop string to translate java field name to db field name
        this.query = query;
        final Mapper mapper = query.getDatastore().getMapper();
        final PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(query.getEntityClass()), fieldName,
            query.isValidatingNames());
        final MappedField mf = pathTarget.getTarget();

        MappedClass mc = null;
        try {
            if (value != null && !ReflectionUtils.isPropertyType(value.getClass())
                && !ReflectionUtils.implementsInterface(value.getClass(), Iterable.class)) {
                if (mf != null && !mf.isTypeMongoCompatible()) {
                    mc = mapper.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubClass());
                } else {
                    mc = mapper.getMappedClass(value);
                }
            }
        } catch (Exception e) {
            // Ignore these. It is likely they related to mapping validation that is unimportant for queries (the query will
            // fail/return-empty anyway)
            LOG.debug("Error during mapping of filter criteria: ", e);
        }

        Object mappedValue = mapper.toMongoObject(mf, mc, value);

        final Class<?> type = (mappedValue == null) ? null : mappedValue.getClass();

        //convert single values into lists for $in/$nin
        if (type != null && (op == FilterOperator.IN || op == FilterOperator.NOT_IN)
            && !type.isArray() && !Iterable.class.isAssignableFrom(type)) {
            mappedValue = Collections.singletonList(mappedValue);
        }

        if (value != null && type == null && (op == FilterOperator.IN || op == FilterOperator.NOT_IN)
            && Iterable.class.isAssignableFrom(value.getClass())) {
            mappedValue = Collections.emptyList();
        }

        this.field = pathTarget.translatedPath();
        this.operator = op;
        this.value = mappedValue;
        this.not = not;
    }

    protected QueryImpl<?> getQuery() {
        return query;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DBObject toDBObject() {
        final DBObject obj = new BasicDBObject();
        if (FilterOperator.EQUAL.equals(operator)) {
            // no operator, prop equals (or NOT equals) value
            if (not) {
                obj.put(field, new BasicDBObject("$not", value));
            } else {
                obj.put(field, value);
            }

        } else {
            final Object object = obj.get(field); // operator within inner object
            Map<String, Object> inner;
            if (!(object instanceof Map)) {
                inner = new HashMap<String, Object>();
                obj.put(field, inner);
            } else {
                inner = (Map<String, Object>) object;
            }

            if (not) {
                inner.put("$not", new BasicDBObject(operator.val(), value));
            } else {
                inner.put(operator.val(), value);
            }
        }
        return obj;
    }

    @Override
    public String getFieldName() {
        return field;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the operator used against this field
     * @see FilterOperator
     */
    public FilterOperator getOperator() {
        return operator;
    }

    /**
     * @return the value used in the Criteria
     */
    public Object getValue() {
        return value;
    }

    /**
     * @return true if 'not' has been applied against this Criteria
     */
    public boolean isNot() {
        return not;
    }

    @Override
    public String toString() {
        return field + " " + operator.val() + " " + value;
    }
}
