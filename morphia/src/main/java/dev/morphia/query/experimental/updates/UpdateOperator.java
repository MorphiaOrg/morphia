package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;

import java.util.List;

/**
 * Defines an update operator
 *
 * @morphia.internal
 * @since 2.0
 */
public class UpdateOperator {
    private final String operator;
    private final String field;
    private Object value;

    protected UpdateOperator(String operator, String field, Object value) {
        if (field == null) {
            throw new UpdateException(Sofia.fieldCannotBeNull());
        }
        if (value == null) {
            throw new UpdateException(Sofia.valueCannotBeNull());
        }
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    protected UpdateOperator(String operator, String field, List<?> values) {
        if (field == null) {
            throw new UpdateException(Sofia.fieldCannotBeNull());
        }
        if (values == null || values.isEmpty()) {
            throw new UpdateException(Sofia.valuesCannotBeNullOrEmpty());
        }
        this.operator = operator;
        this.field = field;
        this.value = values;
    }

    /**
     * @return the field
     * @morphia.internal
     */
    public String field() {
        return field;
    }

    /**
     * @return the operator
     * @morphia.internal
     */
    public String operator() {
        return operator;
    }

    /**
     * Creates the OperationTarget for serialization
     *
     * @param pathTarget the PathTarget
     * @return the OperationTarget
     * @morphia.internal
     */
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value());
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Object value() {
        return value;
    }

    protected void value(Object value) {
        this.value = value;
    }
}
