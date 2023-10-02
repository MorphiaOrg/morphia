package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.FunctionExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

public class FunctionExpressionCodec extends BaseExpressionCodec<FunctionExpression> {
    public FunctionExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, FunctionExpression function, EncoderContext encoderContext) {
        document(writer, function.operation(), () -> {
            writer.writeString("body", function.body());
            array(datastore.getCodecRegistry(), writer, "args", function.args(), encoderContext);
            writer.writeString("lang", function.lang());
        });

    }

    @Override
    public Class<FunctionExpression> getEncoderClass() {
        return FunctionExpression.class;
    }
}
