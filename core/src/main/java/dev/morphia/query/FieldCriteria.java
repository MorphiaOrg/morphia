package dev.morphia.query;


import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * @morphia.internal
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
class FieldCriteria extends AbstractCriteria {
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    private final boolean not;
    private final Mapper mapper;

    FieldCriteria(Mapper mapper, String field, FilterOperator op, Object value, MappedClass mappedClass,
                  boolean validating) {
        this(mapper, field, op, value, false, mappedClass, validating);
    }

    FieldCriteria(Mapper mapper, String fieldName, FilterOperator op, Object value,
                  boolean not, MappedClass mappedClass, boolean validating) {
        this.mapper = mapper;
        final PathTarget pathTarget = new PathTarget(mapper, mappedClass,
            fieldName, validating);

        this.field = pathTarget.translatedPath();

        this.operator = op;
        this.value = ((Document) new OperationTarget(pathTarget, value).encode(mapper)).get(this.field);
        this.not = not;
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
    public Document toDocument() {
        final Document obj = new Document();
        if (FilterOperator.EQUAL.equals(operator)) {
            // no operator, prop equals (or NOT equals) value
            if (not) {
                obj.put(field, new Document("$not", value));
            } else {
                obj.put(field, value);
            }

        } else {
            final Object object = obj.get(field); // operator within inner object
            Map<String, Object> inner;
            if (!(object instanceof Map)) {
                inner = new HashMap<>();
                obj.put(field, inner);
            } else {
                inner = (Map<String, Object>) object;
            }

            if (not) {
                inner.put("$not", new Document(operator.val(), value));
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

    @Override
    public String toString() {
        return field + " " + operator.val() + " " + value;
    }

    protected Mapper getMapper() {
        return mapper;
    }
}
