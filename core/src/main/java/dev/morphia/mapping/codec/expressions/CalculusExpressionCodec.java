package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.CalculusExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Locale;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class CalculusExpressionCodec extends BaseExpressionCodec<CalculusExpression> {
    public CalculusExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, CalculusExpression calculus, EncoderContext encoderContext) {
        document(writer, calculus.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", calculus.input(), encoderContext);
            value(writer, "unit", calculus.unit().name().toLowerCase(Locale.ROOT));
        });

    }

    @Override
    public Class<CalculusExpression> getEncoderClass() {
        return CalculusExpression.class;
    }
}
