package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.TrimExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class TrimExpressionCodec extends BaseExpressionCodec<TrimExpression> {
    public TrimExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, TrimExpression trim, EncoderContext encoderContext) {
        document(writer, trim.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", trim.input(), encoderContext);
            encodeIfNotNull(registry, writer, "chars", trim.chars(), encoderContext);
        });

    }

    @Override
    public Class<TrimExpression> getEncoderClass() {
        return TrimExpression.class;
    }
}
