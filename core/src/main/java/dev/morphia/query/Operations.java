package dev.morphia.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.EntityAware;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class Operations {
    private final Map<String, List<OperationTarget>> ops = new HashMap<>();

    private final EntityModel model;
    private final List<UpdateOperator> updates;
    private final boolean validate;

    /**
     * @param model    the entity model
     * @param updates  the updates
     * @param validate validate or not
     */
    public Operations(@Nullable EntityModel model, List<UpdateOperator> updates, boolean validate) {
        this.model = model;
        this.updates = updates;
        this.updates
                .stream()
                .filter(f -> f instanceof EntityAware)
                .forEach(ea -> ((EntityAware) ea).entityModel(model));
        this.validate = validate;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Operations.class.getSimpleName() + "[", "]")
                .add("ops=" + ops)
                .toString();
    }

    private void versionUpdate(MorphiaDatastore datastore, @Nullable EntityModel entityModel) {
        if (entityModel != null) {
            PropertyModel versionField = entityModel.getVersionProperty();
            if (versionField != null) {
                List<OperationTarget> operationTargets = ops.get("$inc");
                String version = versionField.getMappedName();
                boolean already = operationTargets != null
                        && operationTargets.stream()
                                .anyMatch(tv -> {
                                    PathTarget target = tv.getTarget();
                                    return target != null && target.translatedPath().equals(version);
                                });
                if (!already) {
                    add("$inc", new OperationTarget(new PathTarget(datastore.getMapper(), entityModel, versionField.getName()), 1L));
                }
            }
        }
    }

    /**
     * Add an operator
     *
     * @param operator the operator
     * @param value    the value
     */
    private void add(String operator, OperationTarget value) {
        ops.computeIfAbsent(operator, o -> new ArrayList<>()).add(value);
    }

    /**
     * @param datastore the datastore
     * @return the Document form of this instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Document toDocument(MorphiaDatastore datastore) {
        var document = new Document();
        updates.forEach(update -> {
            Document encoded = encode(datastore, update);
            encoded.forEach((key, value) -> {
                if (!document.containsKey(key)) {
                    document.putAll(encoded);
                } else {
                    Document o = (Document) document.get(key);
                    o.putAll((Document) encoded.get(key));
                }
            });
        });

        return document;
    }

    private Document encode(MorphiaDatastore datastore, UpdateOperator update) {
        var codecRegistry = datastore.getCodecRegistry();
        var writer = new DocumentWriter(datastore.getMapper().getConfig());
        ((Codec) codecRegistry.get(update.getClass()))
                .encode(writer, update, EncoderContext.builder().build());
        return writer.getDocument();
    }

    /**
     * @param datastore the datastore
     * @return the Document form of this instance
     */
    public Document toDocumentOld(MorphiaDatastore datastore) {
        /*
         * maybe i'll come back to the codec solution
         * DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
         * datastore.getCodecRegistry()
         * .get(Operations.class)
         * .encode(writer, this,
         * EncoderContext.builder().build());
         * return writer.getDocument();
         * 
         */
        versionUpdate(datastore, model);
        for (UpdateOperator update : updates) {
            add(update.operator(), update.toOperationTarget(datastore, model, validate));
        }
        Document document = new Document();
        for (Entry<String, List<OperationTarget>> entry : ops.entrySet()) {
            Document targets = new Document();
            for (OperationTarget operationTarget : entry.getValue()) {
                Object encode = operationTarget.encode(datastore);
                if (encode instanceof Document) {
                    targets.putAll((Document) encode);
                } else {
                    document.put(entry.getKey(), encode);
                }
            }
            if (!targets.isEmpty()) {
                document.put(entry.getKey(), targets);
            }
        }
        return document;
    }

}
