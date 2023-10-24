package dev.morphia.query.updates;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;

/**
 * Defines an update operator
 *
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class UpdateOperator {
    private final String operator;
    private final String field;
    private Object value;

    protected UpdateOperator(String operator, String field, Object value) {
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    protected UpdateOperator(String operator, String field, List<?> values) {
        if (values.isEmpty()) {
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
    @MorphiaInternal
    public String field() {
        return field;
    }

    /**
     * @return the operator
     * @morphia.internal
     */
    @MorphiaInternal
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
    @MorphiaInternal
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value());
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @MorphiaInternal
    public Object value() {
        return value;
    }

    protected void value(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("UpdateOperator{operator='%s', field='%s', value=%s}", operator, field, value);
    }
}
