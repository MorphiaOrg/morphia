package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import org.bson.Document;

public class CurrentDateOperator extends UpdateOperator {
    private TypeSpecification typeSpec = TypeSpecification.DATE;

    protected CurrentDateOperator(final String field) {
        super("$currentDate", field, field);
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(pathTarget, typeSpec.toTarget());
    }

    public CurrentDateOperator type(final TypeSpecification typeSpec) {
        this.typeSpec = typeSpec;
        return this;
    }

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
