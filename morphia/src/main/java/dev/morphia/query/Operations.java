package dev.morphia.query;

import dev.morphia.UpdateDocument;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

/**
 * @morphia.internal
 */
class Operations {
    private Map<UpdateOperator, List<OperationTarget>> ops = new HashMap<>();
    private Mapper mapper;
    private MappedClass mappedClass;

    Operations(final Mapper mapper, final MappedClass mappedClass) {
        this.mapper = mapper;
        this.mappedClass = mappedClass;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Operations.class.getSimpleName() + "[", "]")
                   .add("ops=" + ops)
                   .toString();
    }

    /**
     * @return the Document form of this instance
     */
    public Document toDocument() {
        versionUpdate();

        Document document = new Document();
        for (final Entry<UpdateOperator, List<OperationTarget>> entry : ops.entrySet()) {
            Document targets = new Document();
            for (OperationTarget operationTarget : entry.getValue()) {
                Object encode = operationTarget.encode(mapper);
                if (encode instanceof Document) {
                    targets.putAll((Document) encode);
                } else {
                    document.put(entry.getKey().val(), encode);
                }
            }
            if (!targets.isEmpty()) {
                document.put(entry.getKey().val(), targets);
            }
        }
        return document;
    }

    protected void versionUpdate() {
        MappedField versionField = mappedClass.getVersionField();
        if (versionField != null) {
            List<OperationTarget> operationTargets = ops.get(UpdateOperator.INC);
            String version = versionField.getMappedFieldName();
            boolean already = operationTargets != null
                              && operationTargets.stream()
                                                 .anyMatch(tv -> tv.getTarget().translatedPath().equals(version));
            if (!already) {
                add(UpdateOperator.INC, new OperationTarget(new PathTarget(mapper, mappedClass, versionField.getJavaFieldName()), 1L));
            }
        }
    }

    /**
     * Add an operator
     *
     * @param operator the operator
     * @param value    the value
     */
    void add(final UpdateOperator operator, final OperationTarget value) {
        ops.computeIfAbsent(operator, o -> new ArrayList<>()).add(value);
    }

    public void replaceEntity(final Object entity) {
        if (entity == null) {
            throw new UpdateException(Sofia.nullUpdateEntity());
        }
        if (!ops.isEmpty()) {
            throw new UpdateException(Sofia.mixedUpdateOperationsNotAllowed());
        }

        add(UpdateOperator.SET, new OperationTarget(null, new UpdateDocument(entity)));
    }

}
