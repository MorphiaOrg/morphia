package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

/**
 * Defines the $currentDate operator
 *
 * @since 2.0
 */
public class CurrentDateOperator extends UpdateOperator {
    private TypeSpecification type = TypeSpecification.DATE;

    /**
     * Creates an operator for a field
     *
     * @param field the field to update
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected CurrentDateOperator(String field) {
        super("$currentDate", field, field);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);

        return new OperationTarget(pathTarget, type.toTarget());
    }

    /**
     * Sets the type of value to set when updating the field
     *
     * @param type the type to use
     * @return this
     */
    public CurrentDateOperator type(TypeSpecification type) {
        this.type = type;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public TypeSpecification type() {
        return type;
    }

    /**
     * The type options when setting the current date
     */
    public enum TypeSpecification {
        /**
         * the date type
         */
        DATE {
            @Override
            Object toTarget() {
                return true;
            }
        },
        /**
         * the timestamp type
         */
        TIMESTAMP {
            @Override
            Object toTarget() {
                return new Document("$type", "timestamp");
            }
        };

        abstract Object toTarget();
    }
}
