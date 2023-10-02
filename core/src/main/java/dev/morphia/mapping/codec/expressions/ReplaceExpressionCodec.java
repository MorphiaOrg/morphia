package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ReplaceExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ReplaceExpressionCodec extends BaseExpressionCodec<ReplaceExpression> {
    public ReplaceExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ReplaceExpression replace, EncoderContext encoderContext) {
        document(writer, replace.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", replace.input(), encoderContext);
            encodeIfNotNull(registry, writer, "find", replace.find(), encoderContext);
            encodeIfNotNull(registry, writer, "replacement", replace.replacement(), encoderContext);
        });

    }

    @Override
    public Class<ReplaceExpression> getEncoderClass() {
        return ReplaceExpression.class;
    }
}
