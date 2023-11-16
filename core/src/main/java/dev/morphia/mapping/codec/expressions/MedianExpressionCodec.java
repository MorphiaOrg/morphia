package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.MedianExpression;
import dev.morphia.mapping.codec.CodecHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class MedianExpressionCodec extends BaseExpressionCodec<MedianExpression> {
    public MedianExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MedianExpression value, EncoderContext encoderContext) {
        /*
         * $median: {
         * input: "$test01",
         * method: 'approximate'
         * }
         */
        document(writer, value.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", value.value(), encoderContext);
            CodecHelper.value(writer, "method", "approximate");
        });

    }

    @Override
    public Class<MedianExpression> getEncoderClass() {
        return MedianExpression.class;
    }
}
