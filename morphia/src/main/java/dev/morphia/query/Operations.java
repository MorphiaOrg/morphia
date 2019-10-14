package dev.morphia.query;

import dev.morphia.UpdateDocument;
import dev.morphia.annotations.Entity;
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
    private Map<UpdateOperator, List<UpdateTarget>> ops = new HashMap<>();
    private UpdateDocument updateDocument;
    private Mapper mapper;
    private MappedClass mappedClass;

    public Operations(final Mapper mapper, final MappedClass mappedClass) {
        this.mapper = mapper;
        this.mappedClass = mappedClass;
    }

    public void add(final UpdateOperator operator, final UpdateTarget value) {
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
        for (final Entry<UpdateOperator, List<UpdateTarget>> entry : ops.entrySet()) {
            Document targets = new Document();
            for (UpdateTarget updateTarget : entry.getValue()) {
                Object encode = updateTarget.encode(mapper);
                if(encode instanceof Document) {
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
            if (updateDocument != null) {
                updateDocument.skipVersion();
            }
            List<UpdateTarget> updateTargets = ops.get(UpdateOperator.INC);
            boolean already = updateTargets != null
                              && updateTargets.stream()
                                              .noneMatch(tv -> tv.getTarget()
                                                                .translatedPath()
                                                                .equals(versionField.getMappedFieldName()));
            if (!already) {
                add(UpdateOperator.INC, new UpdateTarget(new PathTarget(mapper, mappedClass, versionField.getJavaFieldName()), 1L));
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
        add(UpdateOperator.SET, new UpdateTarget(null, updateDocument));
    }

}
