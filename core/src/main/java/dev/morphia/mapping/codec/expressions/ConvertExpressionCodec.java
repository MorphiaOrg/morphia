package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ConvertExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ConvertExpressionCodec extends BaseExpressionCodec<ConvertExpression> {
    public ConvertExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ConvertExpression convert, EncoderContext encoderContext) {
        document(writer, convert.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", convert.input(), encoderContext);

            writer.writeString("to", convert.to().getName());
            encodeIfNotNull(registry, writer, "onNull", convert.onNull(), encoderContext);
            encodeIfNotNull(registry, writer, "onError", convert.onError(), encoderContext);
        });

    }

    @Override
    public Class<ConvertExpression> getEncoderClass() {
        return ConvertExpression.class;
    }
}
