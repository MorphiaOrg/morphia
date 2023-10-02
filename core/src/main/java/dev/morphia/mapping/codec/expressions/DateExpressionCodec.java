package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.DateExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class DateExpressionCodec extends BaseExpressionCodec<DateExpression> {
    public DateExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateExpression date, EncoderContext encoderContext) {
        encodeIfNotNull(datastore.getCodecRegistry(), writer, date.operation(), date.value(), encoderContext);

    }

    @Override
    public Class<DateExpression> getEncoderClass() {
        return DateExpression.class;
    }
}
