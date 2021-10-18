package dev.morphia.query;


import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
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
    private final Datastore datastore;

    FieldCriteria(Datastore datastore, String field, FilterOperator op, Object value, EntityModel model,
                  boolean validating) {
        this(datastore, field, op, value, false, model, validating);
    }

    FieldCriteria(Datastore datastore,
                  String fieldName,
                  FilterOperator op,
                  Object value,
                  boolean not,
                  EntityModel model,
                  boolean validating) {
        this.datastore = datastore;
        final PathTarget pathTarget = new PathTarget(datastore.getMapper(), model, fieldName, validating);

        this.field = pathTarget.translatedPath();

        this.operator = op;
        this.value = ((Document) new OperationTarget(pathTarget, value).encode(datastore)).get(this.field);
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

    protected Datastore getDatastore() {
        return datastore;
    }
}
