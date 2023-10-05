package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.SwitchExpression;
import dev.morphia.aggregation.expressions.impls.SwitchExpression.Pair;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class SwitchExpressionCodec extends BaseExpressionCodec<SwitchExpression> {
    public SwitchExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SwitchExpression expression, EncoderContext encoderContext) {
        document(writer, expression.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            array(writer, "branches", () -> {
                for (Pair branch : expression.branches()) {
                    document(writer, () -> {
                        encodeIfNotNull(registry, writer, "case", branch.caseExpression(), encoderContext);
                        encodeIfNotNull(registry, writer, "then", branch.then(), encoderContext);
                    });
                }
            });
            encodeIfNotNull(registry, writer, "default", expression.defaultCase(), encoderContext);
        });

    }

    @Override
    public Class<SwitchExpression> getEncoderClass() {
        return SwitchExpression.class;
    }
}
