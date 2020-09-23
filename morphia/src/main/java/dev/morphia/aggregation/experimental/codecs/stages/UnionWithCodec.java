package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.UnionWith;
import dev.morphia.mapping.Mapper;
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
    public UnionWithCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<UnionWith> getEncoderClass() {
        return UnionWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, UnionWith unionWith, EncoderContext encoderContext) {
        String collectionName = unionWith.getCollectionName() != null
                                ? unionWith.getCollectionName()
                                : getMapper().getCollection(unionWith.getCollectionType()).getNamespace().getCollectionName();

        document(writer, () -> {
            value(getMapper(), writer, "coll", collectionName, encoderContext);
            value(getMapper(), writer, "pipeline", unionWith.getStages(), encoderContext);
        });
    }
}
