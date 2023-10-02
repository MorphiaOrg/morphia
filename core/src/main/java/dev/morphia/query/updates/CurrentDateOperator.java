package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

/**
 * Defines the $currentDate operator
 *
 * @since 2.0
 */
public class CurrentDateOperator extends UpdateOperator {
    private TypeSpecification typeSpec = TypeSpecification.DATE;

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
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, typeSpec.toTarget());
    }

    /**
     * Sets the type of value to set when updating the field
     *
     * @param type the type to use
     * @return this
     */
    public CurrentDateOperator type(TypeSpecification type) {
        this.typeSpec = type;
        return this;
    }

    /**
     * Type type options when setting the current date
     */
    public enum TypeSpecification {
        DATE {
            @Override
            Object toTarget() {
                return true;
            }
        },
        TIMESTAMP {
            @Override
            Object toTarget() {
                return new Document("$type", "timestamp");
            }
        };

        abstract Object toTarget();
    }
}
