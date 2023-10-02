package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.stages.ReplaceRoot;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ReplaceRootCodec extends StageCodec<ReplaceRoot> {
    public ReplaceRootCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<ReplaceRoot> getEncoderClass() {
        return ReplaceRoot.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ReplaceRoot replace, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getCodecRegistry();
            writer.writeName("newRoot");
            if (!encodeIfNotNull(registry, writer, replace.getValue(), encoderContext)) {
                DocumentExpression document = replace.getDocument();
                registry.get(DocumentExpression.class)
                        .encode(writer, document, encoderContext);
            }
        });
    }
}
