package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.AccumulatorExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.*;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;

public class AccumulatorExpressionCodec extends BaseExpressionCodec<AccumulatorExpression> {
    public AccumulatorExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, AccumulatorExpression accumulator, EncoderContext encoderContext) {
        document(writer, accumulator.operation(), () -> {
            writer.writeString("init", accumulator.initFunction());
            CodecRegistry registry = datastore.getCodecRegistry();
            array(registry, writer, "initArgs", accumulator.initArgs(), encoderContext);
            writer.writeString("accumulate", accumulator.accumulateFunction());
            array(registry, writer, "accumulateArgs", accumulator.accumulateArgs(), encoderContext);
            writer.writeString("merge", accumulator.mergeFunction());
            writer.writeString("finalize", accumulator.finalizeFunction());
            writer.writeString("lang", accumulator.lang());
        });

    }

    @Override
    public Class<AccumulatorExpression> getEncoderClass() {
        return AccumulatorExpression.class;
    }
}
