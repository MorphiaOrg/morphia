package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ValueExpression;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ValueExpressionCodec extends BaseExpressionCodec<ValueExpression> {

    public ValueExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ValueExpression value, EncoderContext encoderContext) {
        Object object = value.object();
        if (object != null) {
            Codec codec = datastore.getCodecRegistry().get(object.getClass());
            encoderContext.encodeWithChildContext(codec, writer, object);
        } else {
            writer.writeNull();
        }

    }

    @Override
    public Class<ValueExpression> getEncoderClass() {
        return ValueExpression.class;
    }
}
