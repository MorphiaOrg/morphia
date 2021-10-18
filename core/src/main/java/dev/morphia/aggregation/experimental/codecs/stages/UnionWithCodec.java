package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.UnionWith;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * Encodes a UnionWith stage
 *
 * @morphia.internal
 * @since 2.1
 */
public class UnionWithCodec extends StageCodec<UnionWith> {
    /**
     * Creates the codec
     *
     * @param mapper the mapper to use
     * @morphia.internal
     */
    public UnionWithCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<UnionWith> getEncoderClass() {
        return UnionWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, UnionWith unionWith, EncoderContext encoderContext) {
        String name = unionWith.getCollectionName();
        String collectionName = name != null ? name
                                             : getDatastore().getMapper().getEntityModel(unionWith.getCollectionType()).getCollectionName();

        document(writer, () -> {
            value(getDatastore(), writer, "coll", collectionName, encoderContext);
            value(getDatastore(), writer, "pipeline", unionWith.getStages(), encoderContext);
        });
    }
}
