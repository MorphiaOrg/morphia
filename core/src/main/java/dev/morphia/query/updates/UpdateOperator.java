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

    // the field's translated path, worked out on first use and cleared when anything it depends on changes
    @Nullable
    private String translatedField;

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

    /**
     * @return the datastore
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaDatastore datastore() {
        return datastore;
    }

    /**
     * @param datastore the datastore to use
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UpdateOperator datastore(MorphiaDatastore datastore) {
        this.datastore = datastore;
        translatedField = null;
        return this;
    }

    /**
     * @param model the entity model
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public void entityModel(@Nullable EntityModel model) {
        this.model = model;
        translatedField = null;
    }

    /**
     * @return the field
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String field() {
        if (field != null && !field.equals("")) {
            String translated = translatedField;
            if (translated == null) {
                translated = new PathTarget(datastore.getMapper(), model, field, validate).translatedPath();
                translatedField = translated;
            }
            return translated;
        }
        return field;
    }

    /**
     * @return the entity model
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public EntityModel model() {
        return model;
    }

    /**
     * @param model the entity model
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UpdateOperator model(@Nullable EntityModel model) {
        this.model = model;
        translatedField = null;
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
     * @return true if validation is enabled
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean validate() {
        return model != null && validate;
    }

    /**
     * @param validate true to enable validation
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UpdateOperator validate(boolean validate) {
        this.validate = validate;
        translatedField = null;
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
