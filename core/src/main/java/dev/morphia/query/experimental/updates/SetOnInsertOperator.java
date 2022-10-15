package dev.morphia.query.experimental.updates;

import dev.morphia.DatastoreImpl;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityEncoder;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.internal.DatastoreAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.codecs.EncoderContext;

import java.util.Map;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * @morphia.internal
 * @since 2.0
 */
public class SetOnInsertOperator extends UpdateOperator implements DatastoreAware {
    private final Map<String, Object> insertValues;
    private DatastoreImpl datastore;

    /**
     * @param values the values
     * @morphia.internal
     */
    public SetOnInsertOperator(Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = values;
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
    public OperationTarget toTarget(PathTarget pathTarget) {
        Mapper mapper = pathTarget.mapper();
        EntityModel model = mapper.getEntityModel(pathTarget.root().getType());
        MorphiaCodec codec = (MorphiaCodec) mapper.getCodecRegistry().get(model.getType());
        EntityEncoder encoder = codec.getEncoder();
        DocumentWriter writer = new DocumentWriter(mapper);
        document(writer, () -> {
            insertValues.forEach((key, value) -> {
                PathTarget keyTarget = new PathTarget(mapper, model, key, true);
                writer.writeName(keyTarget.translatedPath());
                encoder.encode(writer, value, EncoderContext.builder().build());

            });
        });

        return new OperationTarget(null, null) {
            @Override
            public Object encode(Mapper mapper) {
                return writer.getDocument();
            }
        };
    }
}
