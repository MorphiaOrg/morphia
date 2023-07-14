package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

/**
 * Encodes a UnionWith stage
 *
 * @morphia.internal
 * @since 2.1
 */
@MorphiaInternal
public class UnionWithCodec extends StageCodec<UnionWith> {
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
            value(writer, "coll", collectionName);
            value(getDatastore(), writer, "pipeline", unionWith.getStages(), encoderContext);
        });
    }
}
