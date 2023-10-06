package dev.morphia.mapping.codec.expressions;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Accumulator;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class AccumulatorCodec extends BaseExpressionCodec<Accumulator> {
    public AccumulatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Accumulator accumulator, EncoderContext encoderContext) {
        writer.writeName(accumulator.operation());
        ExpressionList values = accumulator.value();
        if (values != null) {
            CodecRegistry registry = datastore.getCodecRegistry();
            List<Expression> list = values.values();
            if (list.size() == 1) {
                encodeIfNotNull(registry, writer, list.get(0), encoderContext);
            } else {
                array(writer, () -> {
                    for (Expression expression : list) {
                        encodeIfNotNull(registry, writer, expression, encoderContext);
                    }
                });
            }
        } else {
            writer.writeNull();
        }

    }

    @Override
    public Class<Accumulator> getEncoderClass() {
        return Accumulator.class;
    }
}
