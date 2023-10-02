package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.LetExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class LetExpressionCodec extends BaseExpressionCodec<LetExpression> {
    public LetExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, LetExpression let, EncoderContext encoderContext) {
        document(writer, let.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "vars", let.variables(), encoderContext);
            encodeIfNotNull(registry, writer, "in", let.in(), encoderContext);
        });

    }

    @Override
    public Class<LetExpression> getEncoderClass() {
        return LetExpression.class;
    }
}
