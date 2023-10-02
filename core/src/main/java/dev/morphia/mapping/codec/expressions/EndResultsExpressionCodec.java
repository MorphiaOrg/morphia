package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.EndResultsExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class EndResultsExpressionCodec extends BaseExpressionCodec<EndResultsExpression> {
    public EndResultsExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, EndResultsExpression results, EncoderContext encoderContext) {
        document(writer, results.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", results.input(), encoderContext);
            encodeIfNotNull(registry, writer, "n", results.n(), encoderContext);
        });

    }

    @Override
    public Class<EndResultsExpression> getEncoderClass() {
        return EndResultsExpression.class;
    }
}
