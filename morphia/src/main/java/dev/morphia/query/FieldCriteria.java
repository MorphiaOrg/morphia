package dev.morphia.query;


import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyModel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @morphia.internal
 */
class FieldCriteria extends AbstractCriteria {
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    private final boolean not;
    private Mapper mapper;

    FieldCriteria(final Mapper mapper, final QueryImpl<?> query, final String field, final FilterOperator op, final Object value) {
        this(mapper, query, field, op, value, false);
    }

    @SuppressWarnings("deprecation")
    FieldCriteria(final Mapper mapper, final QueryImpl<?> query, final String fieldName, final FilterOperator op, final Object value,
                  final boolean not) {
        this.mapper = mapper;
        final PathTarget pathTarget = new PathTarget(mapper, mapper.getMappedClass(query.getEntityClass()),
            fieldName, query.isValidatingNames());

        this.field = pathTarget.translatedPath();

        this.operator = op;
        this.value = ((Document) new OperationTarget(pathTarget, value).encode(mapper)).get(this.field);
        this.not = not;
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

    protected Mapper getMapper() {
        return mapper;
    }

    @Override
    public String toString() {
        return field + " " + operator.val() + " " + value;
    }
}
