package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ReduceExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ReduceExpressionCodec extends BaseExpressionCodec<ReduceExpression> {
    public ReduceExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ReduceExpression reduce, EncoderContext encoderContext) {
        document(writer, reduce.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", reduce.input(), encoderContext);
            encodeIfNotNull(registry, writer, "initialValue", reduce.initial(), encoderContext);
            encodeIfNotNull(registry, writer, "in", reduce.in(), encoderContext);
        });
    }

    @Override
    public Class<ReduceExpression> getEncoderClass() {
        return ReduceExpression.class;
    }
}
