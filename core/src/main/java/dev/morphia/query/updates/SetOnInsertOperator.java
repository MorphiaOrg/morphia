package dev.morphia.query.updates;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.internal.DatastoreAware;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

/**
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class SetOnInsertOperator extends UpdateOperator implements DatastoreAware {
    private final Map<String, Object> insertValues;
    private DatastoreImpl datastore;

    /**
     * @param values the values
     * @morphia.internal
     */
    @MorphiaInternal
    public SetOnInsertOperator(Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = new LinkedHashMap<>(values);
    }

    /**
     * @morphia.internal
     */
    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setDatastore(DatastoreImpl datastore) {
        this.datastore = datastore;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperationTarget toTarget(PathTarget pathTarget) {
        Mapper mapper = pathTarget.mapper();
        EntityModel model = mapper.getEntityModel(pathTarget.root().getType());
        DocumentWriter writer = new DocumentWriter(mapper.getConfig());
        document(writer, () -> {
            insertValues.forEach((key, value) -> {
                PathTarget keyTarget = new PathTarget(mapper, model, key, true);
                writer.writeName(keyTarget.translatedPath());
                Codec valueCodec = datastore.getCodecRegistry().get(value.getClass());
                valueCodec.encode(writer, value, EncoderContext.builder().build());
            });
        });

        return new OperationTarget(null, null) {
            @Override
            public Object encode(Datastore datastore) {
                return writer.getDocument();
            }
        };
    }
}
