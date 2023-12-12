package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.FilterExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.mapping.codec.CodecHelper.*;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class FilterExpressionCodec extends BaseExpressionCodec<FilterExpression> {
    public FilterExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, FilterExpression value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", value.input(), encoderContext);
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "cond", value.cond(), encoderContext);
            value(writer, "as", value.as());
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "limit", value.limit(), encoderContext);
        });
    }

    @Override
    public Class<FilterExpression> getEncoderClass() {
        return FilterExpression.class;
    }
}
