package dev.morphia.mapping.codec.expressions;

import dev.morphia.aggregation.expressions.impls.SingleValuedExpression;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public abstract class SingleValuedExpressionCodec<T extends SingleValuedExpression> extends BaseExpressionCodec<T> {
}
