package dev.morphia.query.updates;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * Defines an update operator
 *
 * @since 2.0
 */
public class UpdateOperator {
    private final String field;

    private final String operator;

    private MorphiaDatastore datastore = null;

    @Nullable
    private EntityModel model;

    private boolean validate;

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
        this.operator = operator;
        this.field = field;
        this.value = values;
    }

    public MorphiaDatastore datastore() {
        return datastore;
    }

    public UpdateOperator datastore(MorphiaDatastore datastore) {
        this.datastore = datastore;
        return this;
    }

    public void entityModel(@Nullable EntityModel model) {
        this.model = model;
    }

    /**
     * @return the field
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String field() {
        if (field != null && !field.equals("")) {
            return new PathTarget(datastore.getMapper(), model, field, validate).translatedPath();
        }
        return field;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public EntityModel model() {
        return model;
    }

    public UpdateOperator model(@Nullable EntityModel model) {
        this.model = model;
        return this;
    }

    @Override
    public String toString() {
        return "UpdateOperator{" +
                "operator='" + operator + '\'' +
                ", field='" + field + '\'' +
                '}';
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean validate() {
        return model != null && validate;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UpdateOperator validate(boolean validate) {
        this.validate = validate;
        return this;
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
