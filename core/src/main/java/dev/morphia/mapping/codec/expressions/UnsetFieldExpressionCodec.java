package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.UnsetFieldExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class UnsetFieldExpressionCodec extends BaseExpressionCodec<UnsetFieldExpression> {
    public UnsetFieldExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, UnsetFieldExpression unset, EncoderContext encoderContext) {
        document(writer, unset.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "field", unset.field(), encoderContext);
            value(registry, writer, "input", unset.input(), encoderContext);
        });

    }

    @Override
    public Class<UnsetFieldExpression> getEncoderClass() {
        return UnsetFieldExpression.class;
    }
}
