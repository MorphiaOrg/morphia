package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.LogicalExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class LogicalExpressionCodec extends BaseExpressionCodec<LogicalExpression> {
    public LogicalExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, LogicalExpression expression, EncoderContext encoderContext) {
        if (!encodeIfNotNull(datastore.getCodecRegistry(), writer, expression.operation(), expression.value(), encoderContext)) {
            writer.writeNull();
        }
    }

    @Override
    public Class<LogicalExpression> getEncoderClass() {
        return LogicalExpression.class;
    }
}
