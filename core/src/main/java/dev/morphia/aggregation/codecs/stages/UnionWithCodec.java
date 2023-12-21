package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

/**
 * Encodes a UnionWith stage
 *
 * @hidden
 * @morphia.internal
 * @since 2.1
 */
@MorphiaInternal
public class UnionWithCodec extends StageCodec<UnionWith> {
    public UnionWithCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<UnionWith> getEncoderClass() {
        return UnionWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, UnionWith unionWith, EncoderContext encoderContext) {
        String name = unionWith.collectionName();
        String collectionName = name != null ? name
                : getDatastore().getMapper().getEntityModel(unionWith.collectionType()).collectionName();

        if (unionWith.pipeline().isEmpty()) {
            writer.writeString(collectionName);
        } else {
            document(writer, () -> {
                value(writer, "coll", collectionName);
                value(getDatastore().getCodecRegistry(), writer, "pipeline", unionWith.pipeline(), encoderContext);
            });
        }
    }
}
