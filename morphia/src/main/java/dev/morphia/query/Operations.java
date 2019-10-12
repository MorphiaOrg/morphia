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

import static dev.morphia.UpdateDocument.Mode.BODY_ONLY;
import static java.util.stream.Collectors.toList;

class Operations {
    private Map<UpdateOperator, List<TargetValue>> ops = new HashMap<>();
    private UpdateDocument updateDocument;
    private Mapper mapper;
    private MappedClass mappedClass;

    public Operations(final Mapper mapper, final MappedClass mappedClass) {
        this.mapper = mapper;
        this.mappedClass = mappedClass;
    }

    public void add(final UpdateOperator operator, final TargetValue value) {
        ops.computeIfAbsent(operator, o -> new ArrayList<>()).add(value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Operations.class.getSimpleName() + "[", "]")
                   .add("ops=" + ops)
                   .toString();
    }

    public Document toDocument() {
        versionUpdate();

        Document document = new Document();
        for (final Entry<UpdateOperator, List<TargetValue>> entry : ops.entrySet()) {
            var list = entry.getValue().stream()
                            .map(TargetValue::encode)
                            .collect(toList());

            document.put(entry.getKey().val(), list.size() == 1 ? list.get(0) : list);
        }
        return document;
    }

    protected void versionUpdate() {
        MappedField versionField = mappedClass.getVersionField();
        if (versionField != null) {
            if (updateDocument != null) {
                updateDocument.skipVersion();
            }
            List<TargetValue> targetValues = ops.get(UpdateOperator.INC);
            boolean already = targetValues != null
                              && targetValues.stream()
                                             .noneMatch(tv -> tv.getTarget()
                                                                .translatedPath()
                                                                .equals(versionField.getMappedFieldName()));
            if (!already) {
                add(UpdateOperator.INC, new TargetValue(new PathTarget(mapper, mappedClass, versionField.getJavaFieldName()), 1L));
            }
        }
    }

    public void replaceEntity(final Object entity) {
        if (entity == null) {
            throw new UpdateException(Sofia.nullUpdateEntity());
        }
        if (!ops.isEmpty()) {
            throw new UpdateException(Sofia.mixedUpdateOperationsNotAllowed());
        }

        updateDocument = new UpdateDocument(mapper, entity, BODY_ONLY);
        add(UpdateOperator.SET, new TargetValue(null, updateDocument));
    }

}
