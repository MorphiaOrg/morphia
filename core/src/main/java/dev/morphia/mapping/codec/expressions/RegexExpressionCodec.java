package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.RegexExpression;
import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class RegexExpressionCodec extends BaseExpressionCodec<RegexExpression> {
    public RegexExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RegexExpression expression, EncoderContext encoderContext) {
        document(writer, expression.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", expression.input(), encoderContext);
            String regex = expression.regex();
            if (regex != null) {
                writer.writeName("regex");
                Codec<BsonRegularExpression> codec = registry.get(BsonRegularExpression.class);
                codec.encode(writer, new BsonRegularExpression(regex), encoderContext);
            }

            String options = expression.options();
            if (options != null) {
                writer.writeString("options", options);
            }
        });

    }

    @Override
    public Class<RegexExpression> getEncoderClass() {
        return RegexExpression.class;
    }
}
