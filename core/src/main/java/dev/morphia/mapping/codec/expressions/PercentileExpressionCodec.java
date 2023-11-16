package dev.morphia.mapping.codec.expressions;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.PercentileExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class PercentileExpressionCodec extends BaseExpressionCodec<PercentileExpression> {
    public PercentileExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, PercentileExpression percentile, EncoderContext context) {
        /*
         * $percentile: {
         * input: "$test01",
         * p: [ 0.95 ],
         * method: 'approximate'
         * }
         */
        document(writer, "$percentile", () -> {
            if (percentile.inputs().size() == 1) {
                encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", percentile.inputs().get(0), context);
            } else {
                array(writer, "input", () -> {
                    percentile.inputs().forEach(input -> encodeIfNotNull(datastore.getCodecRegistry(), writer, input, context));
                });
            }
            array(writer, "p", () -> {
                List<Expression> percentiles = percentile.percentiles();
                percentiles.forEach(p -> {
                    encodeIfNotNull(datastore.getCodecRegistry(), writer, p, context);
                });
            });
            value(writer, "method", "approximate");
        });
    }

    @Override
    public Class<PercentileExpression> getEncoderClass() {
        return PercentileExpression.class;
    }
}
