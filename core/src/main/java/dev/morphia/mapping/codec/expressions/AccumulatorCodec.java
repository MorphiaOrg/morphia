package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Accumulator ;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;


public class AccumulatorCodec extends BaseExpressionCodec<Accumulator > {
    public AccumulatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Accumulator  accumulator, EncoderContext encoderContext) {
        writer.writeName(accumulator.operation());
        ExpressionList values = accumulator.value();
        if (values != null) {
            Codec<ExpressionList> codec = datastore.getCodecRegistry().get(ExpressionList.class);
            codec.encode(writer, values, encoderContext);
        } else {
            writer.writeNull();
        }

    }

    @Override
    public Class<Accumulator> getEncoderClass() {
        return Accumulator.class;
    }
}
