package dev.morphia.query.updates;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;

/**
 * Defines an update operator
 *
 * @since 2.0
 */
public class UpdateOperator {
    private final String operator;
    private final String field;
    private Object value;

    /**
     * @param operator the operator name
     * @param field    the field to update
     * @param value    the update value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected UpdateOperator(String operator, String field, Object value) {
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    /**
     * @param operator the operator name
     * @param field    the field to update
     * @param values   the update values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String field() {
        return field;
    }

    /**
     * @return the operator
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String operator() {
        return operator;
    }

    /**
     * Creates the OperationTarget for serialization
     *
     * @param datastore the datastore to use
     * @param model     the entity model
     * @param validate  if the target path should be validated
     * @return the OperationTarget
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        return new OperationTarget(new PathTarget(datastore.getMapper(), model, field(), validate), value());
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Object value() {
        return value;
    }

    /**
     * @param value the update value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void value(Object value) {
        this.value = value;
    }
}
